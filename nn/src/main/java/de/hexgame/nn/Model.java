package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Piece;
import de.hexgame.nn.training.ExperienceBuffer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.AllocationPolicy;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import static de.hexgame.logic.GameState.BOARD_SIZE;

@Slf4j
@SuppressWarnings("resource")
public class Model extends Thread {
    private static final int NUM_INPUT_CHANNELS = 5;
    private static final int BATCH_SIZE = 512;
    private static final WorkspaceConfiguration workspaceConfig = WorkspaceConfiguration.builder()
            .initialSize(2L * 1024 * 1024)
            .policyAllocation(AllocationPolicy.STRICT)
            .build();
    private static final String workspaceId = "model-gpu";

    private final ComputationGraph computationGraph;
    private final Map<GameState, Output> cache = Collections.synchronizedMap(new WeakHashMap<>());
    private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();

    @SneakyThrows
    public Model(File file, boolean loadUpdater) {
        setDaemon(true);
        computationGraph = ComputationGraph.load(file, loadUpdater);
    }

    public Model() {
        setDaemon(true);
        ComputationGraphConfiguration.GraphBuilder config = new NeuralNetConfiguration.Builder()
                .activation(Activation.IDENTITY)
                .weightInit(WeightInit.RELU)
                .updater(new Adam(1e-3))
                .weightDecay(1e-4)
                .graphBuilder()
                .addInputs("input")
                .setInputTypes(InputType.convolutional(BOARD_SIZE, BOARD_SIZE, NUM_INPUT_CHANNELS));

        String inName = addConvBatchNormBlock(config, "init", "input", true);
        for (int i = 0; i < 12; i++) {
            inName = addResidualBlock(config, i, inName);
        }

        config.addLayer("policyConv", new ConvolutionLayer.Builder(1, 1)
                        .nOut(2)
                        .convolutionMode(ConvolutionMode.Same)
                        .build(), inName)
//                .addLayer("policyNorm", new BatchNormalization.Builder()
//                        .build(), "policyConv")
                .addLayer("policyRelu", new ActivationLayer.Builder()
                        .activation(Activation.RELU)
                        .build(), "policyConv")
                .addLayer("policyOut", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .nOut(BOARD_SIZE * BOARD_SIZE)
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .build(), "policyRelu")

                .addLayer("valueConv", new ConvolutionLayer.Builder(1, 1)
                        .nOut(1)
                        .convolutionMode(ConvolutionMode.Same)
                        .build(), inName)
//                .addLayer("valueNorm", new BatchNormalization.Builder()
//                        .build(), "valueConv")
                .addLayer("valueRelu", new ActivationLayer.Builder()
                        .activation(Activation.RELU)
                        .build(), "valueConv")
                .addLayer("valueFC", new DenseLayer.Builder()
                        .nOut(128)
                        .build(), "valueRelu")
//                .addLayer("valueDenseNorm", new BatchNormalization.Builder()
//                        .build(), "valueFC")
                .addLayer("valueDenseRelu", new ActivationLayer.Builder()
                        .activation(Activation.RELU)
                        .build(), "valueFC")
                .addLayer("valueOut", new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nOut(1)
                        .activation(Activation.TANH)
                        .weightInit(WeightInit.XAVIER)
                        .build(), "valueDenseRelu")

                .setOutputs("policyOut", "valueOut");

        computationGraph = new ComputationGraph(config.build());
        computationGraph.init();
    }

    private String addConvBatchNormBlock(ComputationGraphConfiguration.GraphBuilder config, String blockName, String inName, boolean useActivation) {
        String convName = "conv_" + blockName;
        String bnName = "batch_norm_" + blockName;
        String actName = "relu_" + blockName;

        config.addLayer(convName, new ConvolutionLayer.Builder(3, 3).nOut(64).convolutionMode(ConvolutionMode.Same).build(), inName);
       // config.addLayer(bnName, new BatchNormalization.Builder().nOut(64).build(), convName);
        if (useActivation) {
            config.addLayer(actName, new ActivationLayer.Builder().activation(Activation.RELU).build(), convName);
            return actName;
        } else {
            return convName;
        }
    }

    private String addResidualBlock(ComputationGraphConfiguration.GraphBuilder config, int blockNumber, String inName) {
        String firstBlock = "residual_1_" + blockNumber;
        String secondBlock = "residual_2_" + blockNumber;
        String mergeBlock = "add_" + blockNumber;
        String actBlock = "relu_" + blockNumber;

        String firstBnOut = addConvBatchNormBlock(config, firstBlock, inName, true);
        String secondBnOut = addConvBatchNormBlock(config, secondBlock, firstBnOut, false);
        config.addVertex(mergeBlock, new ElementWiseVertex(ElementWiseVertex.Op.Add), inName, secondBnOut);
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

    public void fit(ExperienceBuffer experienceBuffer, int numBatches) {
        for (int i = 0; i < numBatches; i++) {
            org.nd4j.linalg.dataset.api.MultiDataSet dataSet = experienceBuffer.sample(BATCH_SIZE);
            // Mirror half of the data to improve model generalisation
            INDArray newFeatures = dataSet.getFeatures(0);
            INDArray features = newFeatures.dup();
            INDArray newPolicy = dataSet.getLabels(0);
            INDArray policy = newPolicy.dup();
            for (int sampleIndex = 0; sampleIndex < BATCH_SIZE / 2; sampleIndex++) {
                for (int row = 0; row < BOARD_SIZE; row++) {
                    int invertedRow = BOARD_SIZE - 1 - row;
                    for (int column = 0; column < BOARD_SIZE; column++) {
                        int invertedColumn = BOARD_SIZE - 1 - column;
                        newFeatures.putScalar(sampleIndex, 0, invertedRow, invertedColumn,
                                features.getFloat(sampleIndex, 0, row, column));
                        newFeatures.putScalar(sampleIndex, 1, invertedRow, invertedColumn,
                                features.getFloat(sampleIndex, 1, row, column));
                        newPolicy.putScalar(sampleIndex, (long) invertedRow * BOARD_SIZE + invertedColumn,
                                policy.getFloat(sampleIndex, row * BOARD_SIZE + column));
                    }
                }
            }
            computationGraph.fit(dataSet);
        }
    }

    @SneakyThrows
    public void save(File file) {
        computationGraph.save(file);
    }

    private void extractFeatures(List<GameState> gameStates, INDArray featuresOut) {
        featuresOut.assign(0.0f);

        for (int i = 0; i < gameStates.size(); i++) {
            final GameState gameState = gameStates.get(i);

            INDArray ownPieces = featuresOut.get(NDArrayIndex.point(i), NDArrayIndex.point(0));
            INDArray enemyPieces = featuresOut.get(NDArrayIndex.point(i), NDArrayIndex.point(1));
            INDArray swapPossible = featuresOut.get(NDArrayIndex.point(i), NDArrayIndex.point(2));

            FloatBuffer fbOwn = ownPieces.data().asNioFloat();
            FloatBuffer fbEnemy = enemyPieces.data().asNioFloat();
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
                    ownPieces.putScalar(eqRow, eqCol, 1.0f);
                } else {
                    enemyPieces.putScalar(eqRow, eqCol, 1.0f);
                }
            }

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
        final INDArray input = Nd4j.create(BATCH_SIZE, NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE);
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
