package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Piece;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ActivationLayer;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.concurrency.AffinityManager;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.AllocationPolicy;
import org.nd4j.linalg.api.memory.enums.ResetPolicy;
import org.nd4j.linalg.api.memory.enums.SpillPolicy;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import static de.hexgame.logic.GameState.BOARD_SIZE;

@Slf4j
@SuppressWarnings("resource")
public class Model extends Thread {
    private static final int NUM_INPUT_CHANNELS = 6;
    private static final int BATCH_SIZE = 128;
    private static final WorkspaceConfiguration workspaceConfig = WorkspaceConfiguration.builder()
            .initialSize(256L * 1024 * 1024)
            .policyAllocation(AllocationPolicy.STRICT)
            .policyReset(ResetPolicy.BLOCK_LEFT)
            .policySpill(SpillPolicy.FAIL)
            .build();
    private static final String workspaceId = "model-gpu";

    private final ComputationGraph computationGraph;
    private final Map<GameState, Output> cache = Collections.synchronizedMap(new WeakHashMap<>());
    private final BlockingQueue<Task> taskQueue = new ArrayBlockingQueue<>(256);

    @SneakyThrows
    public Model(File file, boolean loadUpdater) {
        computationGraph = ComputationGraph.load(file, loadUpdater);
    }

    public Model() {
        setDaemon(true);
        ComputationGraphConfiguration.GraphBuilder config = new NeuralNetConfiguration.Builder()
                .updater(new Adam(1e-3))
                .graphBuilder()
                .addInputs("input")
                .setInputTypes(InputType.convolutional(BOARD_SIZE, BOARD_SIZE, NUM_INPUT_CHANNELS))
                .addLayer("inputConv", new ConvolutionLayer.Builder(3, 3).convolutionMode(ConvolutionMode.Same).nOut(64).build(), "input");

        String inName = "inputConv";
        for (int i = 0; i < 12; i++) {
            inName = addResidualBlock(config, i, inName);
        }

        config.addLayer("policyConv", new ConvolutionLayer.Builder(1, 1)
                        .nOut(2)
                        .convolutionMode(ConvolutionMode.Same)
                        .activation(Activation.RELU)
                        .build(), inName)
                .addLayer("policyOut", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .nOut(BOARD_SIZE * BOARD_SIZE)
                        .activation(Activation.SOFTMAX)
                        .build(), "policyConv")

                .addLayer("valueConv", new ConvolutionLayer.Builder(1, 1)
                        .nOut(1)
                        .convolutionMode(ConvolutionMode.Same)
                        .activation(Activation.RELU)
                        .build(), inName)
                .addLayer("valueOut", new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nOut(1)
                        .activation(Activation.TANH)
                        .build(), "valueConv")

                .setOutputs("policyOut", "valueOut");

        computationGraph = new ComputationGraph(config.build());
        computationGraph.init();
    }

    private String addConvBatchNormBlock(ComputationGraphConfiguration.GraphBuilder config, String blockName, String inName, boolean useActivation) {
        String convName = "conv_" + blockName;
        String bnName = "batch_norm_" + blockName;
        String actName = "relu_" + blockName;

        config.addLayer(convName, new ConvolutionLayer.Builder(3, 3).nOut(64).convolutionMode(ConvolutionMode.Same).activation(Activation.IDENTITY).build(), inName);
        config.addLayer(bnName, new BatchNormalization.Builder().nOut(64).build(), convName);
        if (useActivation) {
            config.addLayer(actName, new ActivationLayer.Builder().activation(Activation.RELU).build(), bnName);
            return actName;
        } else {
            return bnName;
        }
    }

    private String addResidualBlock(ComputationGraphConfiguration.GraphBuilder config, int blockNumber, String inName) {
        String firstBlock = "residual_1_" + blockNumber;
        String firstOut = "relu_residual_1_" + blockNumber;
        String secondBlock = "residual_2_" + blockNumber;
        String mergeBlock = "add_" + blockNumber;
        String actBlock = "relu_" + blockNumber;

        String firstBnOut = addConvBatchNormBlock(config, firstBlock, inName, true);
        String secondBnOut = addConvBatchNormBlock(config, secondBlock, firstOut, false);
        config.addVertex(mergeBlock, new ElementWiseVertex(ElementWiseVertex.Op.Add), firstBnOut, secondBnOut);
        config.addLayer(actBlock, new ActivationLayer.Builder().activation(Activation.RELU).build(), mergeBlock);
        return actBlock;
    }

    public CompletableFuture<Output> predict(GameState gameState) {
        Output cachedOutput = cache.get(gameState);
        if (cachedOutput != null) {
            return CompletableFuture.completedFuture(cachedOutput);
        }

        Task task = new Task(gameState, new CompletableFuture<>());
        taskQueue.add(task);
        return task.future;
    }

    public void fit(MultiDataSetIterator dataSetIterator, int numEpochs) {
        computationGraph.fit(dataSetIterator, numEpochs);
    }

    @SneakyThrows
    public void save(File file) {
        computationGraph.save(file);
    }

    private void extractFeatures(List<GameState> gameStates, INDArray featuresOut) {
        INDArray ownPiecesHost = Nd4j.createUninitializedDetached(Nd4j.defaultFloatingPointType(), BOARD_SIZE, BOARD_SIZE);
        INDArray enemyPiecesHost = Nd4j.createUninitializedDetached(Nd4j.defaultFloatingPointType(), BOARD_SIZE, BOARD_SIZE);

        for (int i = 0; i < gameStates.size(); i++) {
            final GameState gameState = gameStates.get(i);

            INDArray ownPieces = featuresOut.get(NDArrayIndex.point(i), NDArrayIndex.point(0));
            INDArray enemyPieces = featuresOut.get(NDArrayIndex.point(i), NDArrayIndex.point(1));
            INDArray swapPossible = featuresOut.get(NDArrayIndex.point(i), NDArrayIndex.point(2));

            Nd4j.getAffinityManager().tagLocation(ownPiecesHost, AffinityManager.Location.HOST);
            Nd4j.getAffinityManager().tagLocation(enemyPiecesHost, AffinityManager.Location.HOST);
            FloatBuffer fbOwn = ownPiecesHost.data().asNioFloat();
            FloatBuffer fbEnemy = enemyPiecesHost.data().asNioFloat();
            for (int k = 0; k < fbOwn.capacity(); k++) {
                fbOwn.put(k, 0f);
                fbEnemy.put(k, 0f);
            }

            for (int flat = 0; flat < BOARD_SIZE * BOARD_SIZE; flat++) {
                Piece p = gameState.getPiece(flat);
                if (p == null) continue;

                int equalizedIndex = equalizeIndex(flat, gameState.getSideToMove());
                int eqRow = equalizedIndex / BOARD_SIZE;
                int eqCol = equalizedIndex % BOARD_SIZE;

                if (p.getColor() == gameState.getSideToMove()) {
                    ownPiecesHost.putScalar(eqRow, eqCol, 1.0f);
                } else {
                    enemyPiecesHost.putScalar(eqRow, eqCol, 1.0f);
                }
            }

            ownPieces.assign(ownPiecesHost);
            enemyPieces.assign(enemyPiecesHost);

            if (gameState.getHalfMoveCounter() == 1) {
                swapPossible.assign(1.0f);
            }
        }

        featuresOut.get(NDArrayIndex.all(), NDArrayIndex.point(3), NDArrayIndex.point(0), NDArrayIndex.all()).assign(1.0f);
        featuresOut.get(NDArrayIndex.all(), NDArrayIndex.point(3), NDArrayIndex.point(BOARD_SIZE - 1), NDArrayIndex.all()).assign(1.0f);

        featuresOut.get(NDArrayIndex.all(), NDArrayIndex.point(4), NDArrayIndex.all(), NDArrayIndex.point(0)).assign(1.0f);
        featuresOut.get(NDArrayIndex.all(), NDArrayIndex.point(4), NDArrayIndex.all(), NDArrayIndex.point(BOARD_SIZE - 1)).assign(1.0f);
    }

    public MultiDataSet createDataSet(GameState gameState, Output output) {
        INDArray features = Nd4j.create(1, NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE);
        extractFeatures(List.of(gameState), features);
        final float[] policyJvm = new float[BOARD_SIZE * BOARD_SIZE];
        for (int flat = 0; flat < BOARD_SIZE * BOARD_SIZE; flat++) {
            policyJvm[equalizeIndex(flat, gameState.getSideToMove())] = output.policy()[flat];
        }
        INDArray policy = Nd4j.create(policyJvm, new int[]{1, BOARD_SIZE * BOARD_SIZE});
        INDArray value = Nd4j.createFromArray(output.value()).reshape(1, 1);

        return new MultiDataSet(
                new INDArray[]{features},
                new INDArray[]{policy, value}
        );
    }

    private int equalizeIndex(int index, Piece.Color sideToMove) {
        final boolean redToMove = sideToMove == Piece.Color.RED;

        int row = index / BOARD_SIZE;
        int col = index % BOARD_SIZE;
        int canRow, canCol;

        if (redToMove) {
            canRow = row;
            canCol = col;
        } else {
            canRow = col;
            canCol = row;
        }

        return canRow * BOARD_SIZE + canCol;
    }

    static long c = 0;
    static long lastPrintTime = 0;
    @Override
    public void run() {
        final List<Task> tasks = new ArrayList<>();
        while (true) {
            try (MemoryWorkspace workspace = Nd4j.getWorkspaceManager().getAndActivateWorkspace(workspaceConfig, workspaceId)) {
                try {
                    tasks.add(taskQueue.take());
                } catch (InterruptedException e) {
                    return;
                }
                taskQueue.drainTo(tasks, BATCH_SIZE - 1);
                if (System.currentTimeMillis() - lastPrintTime > 1000) {
                    lastPrintTime = System.currentTimeMillis();
                    log.info("Total task count: {}, Batch task count: {}", c, tasks.size());
                }
                c += tasks.size();
                final INDArray input = Nd4j.create(tasks.size(), NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE);
                extractFeatures(tasks.stream().map(Task::gameState).toList(), input);
                final INDArray[] outputs = computationGraph.output(false, workspace, input);
                final INDArray policies = outputs[0];
                final INDArray values = outputs[1];
                for (int i = 0; i < tasks.size(); i++) {
                    final Task task = tasks.get(i);
                    final INDArray policy = policies.get(NDArrayIndex.point(i));
                    final float[] policyJvm = new float[BOARD_SIZE * BOARD_SIZE];
                    for (int flat = 0; flat < BOARD_SIZE * BOARD_SIZE; flat++) {
                        policyJvm[flat] = policy.getFloat(equalizeIndex(flat, task.gameState.getSideToMove()));
                    }
                    final Output output = new Output(
                            policyJvm,
                            values.getFloat(i)
                    );
                    cache.put(task.gameState, output);
                    task.future.complete(output);
                }
            }
            tasks.clear();
        }
    }

    public record Output(float[] policy, float value) implements Cloneable {
        @Override
        public Output clone() {
            try {
                return (Output) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    private record Task(GameState gameState, CompletableFuture<Output> future) {
    }
}
