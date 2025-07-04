package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Piece;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

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

    private final ComputationGraph computationGraph;
    private final Map<GameState, Output> cache = Collections.synchronizedMap(new WeakHashMap<>());
    private final BlockingQueue<Task> taskQueue = new ArrayBlockingQueue<>(256);

    public Model() {
        setDaemon(true);
        ComputationGraphConfiguration config = new NeuralNetConfiguration.Builder()
                .updater(new Adam(1e-3))
                .graphBuilder()
                .addInputs("input")
                .setInputTypes(InputType.convolutional(BOARD_SIZE, BOARD_SIZE, NUM_INPUT_CHANNELS))
                .addLayer("conv1", new ConvolutionLayer.Builder(3, 3)
                        .nOut(64)
                        .padding(1, 1)
                        .activation(Activation.RELU)
                        .build(), "input")
                .addLayer("conv2", new ConvolutionLayer.Builder(3, 3)
                        .nOut(64)
                        .padding(1, 1)
                        .activation(Activation.RELU)
                        .build(), "conv1")

                .addLayer("policyConv", new ConvolutionLayer.Builder(1, 1)
                        .nOut(2)
                        .activation(Activation.RELU)
                        .build(), "conv2")
                .addLayer("policyOut", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .nOut(BOARD_SIZE * BOARD_SIZE)
                        .activation(Activation.SOFTMAX)
                        .build(), "policyConv")

                .addLayer("valueConv", new ConvolutionLayer.Builder(1, 1)
                        .nOut(1)
                        .activation(Activation.RELU)
                        .build(), "conv2")
                .addLayer("valueOut", new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nOut(1)
                        .activation(Activation.TANH)
                        .build(), "valueConv")

                .setOutputs("policyOut", "valueOut")
                .build();

        computationGraph = new ComputationGraph(config);
        computationGraph.init();
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

    public void extractFeatures(GameState gameState, INDArray featuresOut) {
        featuresOut.assign(0);

        INDArray ownPieces = featuresOut.get(NDArrayIndex.point(0));
        INDArray enemyPieces = featuresOut.get(NDArrayIndex.point(1));
        INDArray swapPossible = featuresOut.get(NDArrayIndex.point(2));
        INDArray redTargets = featuresOut.get(NDArrayIndex.point(3));
        INDArray blueTargets = featuresOut.get(NDArrayIndex.point(4));

        final boolean redToMove = (gameState.getSideToMove() == Piece.Color.RED);

        for (int flat = 0; flat < BOARD_SIZE * BOARD_SIZE; flat++) {
            Piece p = gameState.getPiece(flat);
            if (p == null) continue;

            int row = flat / BOARD_SIZE;
            int col = flat % BOARD_SIZE;
            int canRow, canCol;

            if (redToMove) {
                canRow = row;
                canCol = col;
            } else {
                canRow = BOARD_SIZE - 1 - col;
                canCol = row;
            }

            if (p.getColor() == gameState.getSideToMove()) {
                ownPieces.putScalar(canRow, canCol, 1.0f);
            } else {
                enemyPieces.putScalar(canRow, canCol, 1.0f);
            }
        }

        if (gameState.getHalfMoveCounter() == 1) {
            swapPossible.assign(1.0f);
        }

        redTargets.get(NDArrayIndex.point(0), NDArrayIndex.all()).assign(1.0f);
        redTargets.get(NDArrayIndex.point(BOARD_SIZE - 1), NDArrayIndex.all()).assign(1.0f);

        blueTargets.get(NDArrayIndex.all(), NDArrayIndex.point(0)).assign(1.0f);
        blueTargets.get(NDArrayIndex.all(), NDArrayIndex.point(BOARD_SIZE - 1)).assign(1.0f);
    }

    public MultiDataSet createDataSet(GameState gameState, Output output) {
        INDArray features = Nd4j.create(NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE);
        extractFeatures(gameState, features);
        INDArray policy = Nd4j.create(output.policy(), new int[]{1, BOARD_SIZE, BOARD_SIZE});
        INDArray value = Nd4j.createFromArray(output.value());

        return new MultiDataSet(
                new INDArray[]{features.reshape(1, NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE)},
                new INDArray[]{policy, value.reshape(1, 1)}
        );
    }

    static long c = 0;
    static long lastPrintTime = 0;
    @Override
    public void run() {
        final INDArray inputBuffer = Nd4j.create(BATCH_SIZE, NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE);
        final List<Task> tasks = new ArrayList<>();
        while (true) {
            try {
                tasks.add(taskQueue.take());
            } catch (InterruptedException e) {
                return;
            }
            taskQueue.drainTo(tasks, BATCH_SIZE - 1);
            tasks.removeIf(task -> {
                Output cachedOutput = cache.get(task.gameState);
                if (cachedOutput != null) {
                    task.future.complete(cachedOutput);
                    return true;
                }
                return false;
            });
            if (System.currentTimeMillis() - lastPrintTime > 1000) {
                lastPrintTime = System.currentTimeMillis();
                log.info("Total task count: {}, Batch task count: {}", c, tasks.size());
            }
            c += tasks.size();
            final INDArray input = inputBuffer.get(NDArrayIndex.interval(0, tasks.size()));
            for (int i = 0; i < tasks.size(); i++) {
                extractFeatures(tasks.get(i).gameState, input.get(NDArrayIndex.point(i)));
            }
            final INDArray[] outputs = computationGraph.output(input);
            final INDArray policies = outputs[0];
            final INDArray values = outputs[1];
            for (int i = 0; i < tasks.size(); i++) {
                final Task task = tasks.get(i);
                final Output output = new Output(
                        policies.get(NDArrayIndex.point(i)).ravel().toFloatVector(),
                        values.getFloat(i)
                );
                cache.put(task.gameState, output);
                task.future.complete(output);
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
