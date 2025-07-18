package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Piece;
import de.hexgame.nn.training.ExperienceBuffer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tensorflow.*;
import org.tensorflow.framework.optimizers.Momentum;
import org.tensorflow.framework.optimizers.Optimizer;
import org.tensorflow.ndarray.BooleanNdArray;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.op.Ops;
import org.tensorflow.op.core.Placeholder;
import org.tensorflow.op.core.Variable;
import org.tensorflow.op.nn.Conv2d;
import org.tensorflow.op.nn.FusedBatchNorm;
import org.tensorflow.proto.data.Dataset;
import org.tensorflow.types.TBool;
import org.tensorflow.types.TFloat32;

import java.io.Closeable;
import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import static de.hexgame.logic.GameState.BOARD_SIZE;

@Slf4j
public class Model extends Thread implements Closeable {
    private static final String PADDING_TYPE = "SAME";
    private static final String TRAIN = "train";
    private static final int CHANNELS = 64;
    private static final int NUM_INPUT_CHANNELS = 5;
    private static final int BATCH_SIZE = 512;
    static long c = 0;
    static long lastPrintTime = 0;
    private final Graph graph;
    private final Session session;
    private final Map<GameState, Output> cache = Collections.synchronizedMap(new WeakHashMap<>());
    private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();

    @SneakyThrows
    public Model(File file, boolean loadUpdater) {
        setDaemon(true);
        SavedModelBundle modelBundle = SavedModelBundle.load(file.getAbsolutePath());
        graph = modelBundle.graph();
        session = modelBundle.session();
    }

    public Model() {
        setDaemon(true);
        graph = new Graph();

        Ops tf = Ops.create(graph);

        Placeholder<TBool> input = tf.withName("input").placeholder(TBool.class,
                Placeholder.shape(Shape.of(-1, NUM_INPUT_CHANNELS, BOARD_SIZE * BOARD_SIZE)));
        Placeholder<TFloat32> policyLabels = tf.withName("policyLabels").placeholder(TFloat32.class,
                Placeholder.shape(Shape.of(-1, BOARD_SIZE * BOARD_SIZE)));
        Placeholder<TFloat32> valueLabels = tf.withName("valueLabels").placeholder(TFloat32.class);

        Operand<TFloat32> operand = addConvBatchNormBlock(tf, tf.dtypes.cast(input, TFloat32.class), 3, CHANNELS, true);
        for (int i = 0; i < 12; i++) {
            operand = addResidualBlock(tf, operand);
        }

        Operand<TFloat32> policyRelu = addConvBatchNormBlock(tf, operand, 1, 2, true);
        Operand<TFloat32> policyFlat = tf.reshape(policyRelu, tf.array(-1, 2 * BOARD_SIZE * BOARD_SIZE));
        Variable<TFloat32> policyFcWeights = tf.variable(tf.math.mul(tf.random
                        .truncatedNormal(tf.array(2 * BOARD_SIZE * BOARD_SIZE, BOARD_SIZE * BOARD_SIZE), TFloat32.class),
                tf.constant(0.1f)));
        Variable<TFloat32> policyFcBiases = tf.variable(tf.zeros(tf.array(BOARD_SIZE * BOARD_SIZE), TFloat32.class));
        Operand<TFloat32> policyLogits = tf.math.add(tf.linalg.matMul(policyFlat, policyFcWeights), policyFcBiases);
        tf.withName("policyOut").nn.softmax(policyLogits);

        Operand<TFloat32> valueRelu = addConvBatchNormBlock(tf, operand, 1, 1, true);
        Operand<TFloat32> valueFlat = tf.reshape(valueRelu, tf.array(-1, BOARD_SIZE * BOARD_SIZE));
        Variable<TFloat32> valueFcWeights = tf.variable(tf.math.mul(tf.random
                        .truncatedNormal(tf.array(BOARD_SIZE * BOARD_SIZE, 256), TFloat32.class),
                tf.constant(0.1f)));
        Variable<TFloat32> valueFcBiases = tf.variable(tf.zeros(tf.array(256), TFloat32.class));
        Operand<TFloat32> valueFc = tf.math.add(tf.linalg.matMul(valueFlat, valueFcWeights), valueFcBiases);
        Variable<TFloat32> valueOutWeights = tf.variable(tf.math.mul(tf.random
                        .truncatedNormal(tf.array(256, 1), TFloat32.class),
                tf.constant(0.1f)));
        Variable<TFloat32> valueOutBias = tf.variable(tf.zeros(tf.array(1), TFloat32.class));
        Operand<TFloat32> valueOut = tf.withName("valueOut").math.tanh(tf.math.add(tf.linalg.matMul(valueFc, valueOutWeights), valueOutBias));

        Operand<TFloat32> loss = tf.math.add(tf.nn.softmaxCrossEntropyWithLogits(policyLogits, policyLabels).loss(),
                tf.math.mean(tf.math.square(tf.math.sub(valueOut, valueLabels)), tf.constant(0)));

        Optimizer optimizer = new Momentum(graph, 1e-2f, 0.9f);
        optimizer.minimize(loss, TRAIN);

        session = new Session(graph);
    }

    private Operand<TFloat32> addConvBatchNormBlock(Ops tf, Operand<TFloat32> input, int width, int channels, boolean useActivation) {
        Variable<TFloat32> convWeights = tf.variable(tf.math.mul(tf.random
                        .truncatedNormal(tf.array(width, width, NUM_INPUT_CHANNELS, channels), TFloat32.class),
                tf.constant(0.1f)));
        Conv2d<TFloat32> conv = tf.nn.conv2d(input, convWeights, Arrays.asList(1L, 1L, 1L, 1L), PADDING_TYPE);

        Variable<TFloat32> mean = tf.variable(tf.zeros(tf.array(channels), TFloat32.class));
        Variable<TFloat32> variance = tf.variable(tf.ones(tf.array(channels), TFloat32.class));

        Variable<TFloat32> scale = tf.variable(tf.ones(tf.array(channels), TFloat32.class));
        Variable<TFloat32> offset = tf.variable(tf.zeros(tf.array(channels), TFloat32.class));

        FusedBatchNorm<TFloat32, TFloat32> batchNorm = tf.nn.fusedBatchNorm(conv, scale, offset, mean, variance);

        if (useActivation) {
            return tf.nn.relu(batchNorm.op().output(0));
        } else {
            return batchNorm.op().output(0);
        }
    }

    private Operand<TFloat32> addResidualBlock(Ops tf, Operand<TFloat32> input) {
        Operand<TFloat32> firstBnOut = addConvBatchNormBlock(tf, input, 3, CHANNELS, true);
        Operand<TFloat32> secondBnOut = addConvBatchNormBlock(tf, firstBnOut, 3, CHANNELS, false);
        return tf.nn.relu(tf.math.add(input, secondBnOut));
    }

    public CompletableFuture<Output> predict(GameState gameState) {
        Output cachedOutput = cache.get(gameState);
        if (cachedOutput != null) {
            return CompletableFuture.completedFuture(cachedOutput);
        }

        Task task = new Task(gameState.clone(), new CompletableFuture<>());
        taskQueue.add(task);
        return task.future;
    }

    public void fit(ExperienceBuffer experienceBuffer, int numBatches) {
        for (int i = 0; i < numBatches; i++) {
        }
    }

    @SneakyThrows
    public void save(File file) {
        SavedModelBundle.exporter(file.getAbsolutePath())
                .withSession(session)
                .export();
    }

    private void extractFeatures(List<GameState> gameStates, BooleanNdArray featuresOut) {
        featuresOut.scalars().forEach(b -> b.setBoolean(false));

        for (int i = 0; i < gameStates.size(); i++) {
            GameState gameState = gameStates.get(i);

            BooleanNdArray ownPieces = featuresOut.get(i, 0);
            BooleanNdArray enemyPieces = featuresOut.get(i, 1);
            BooleanNdArray swapPossible = featuresOut.get(i, 2);
            BooleanNdArray ownTargets = featuresOut.get(i, 3);
            BooleanNdArray enemyTargets = featuresOut.get(i, 4);

            for (int hexIndex = 0; hexIndex < BOARD_SIZE * BOARD_SIZE; hexIndex++) {
                int column = hexIndex % BOARD_SIZE;
                if (column == 0 || column == BOARD_SIZE - 1) {
                    ownTargets.setBoolean(true, hexIndex);
                }

                int row = hexIndex / BOARD_SIZE;
                if (row == 0 || row == BOARD_SIZE - 1) {
                    enemyTargets.setBoolean(true, hexIndex);
                }

                Piece piece = gameState.getPiece(hexIndex);
                if (piece == null) continue;

                int eqIndex = equalizeIndex(hexIndex, gameState.getSideToMove());

                if (piece.getColor() == gameState.getSideToMove()) {
                    ownPieces.setBoolean(true, eqIndex);
                } else {
                    enemyPieces.setBoolean(true, eqIndex);
                }
            }

            if (gameState.getHalfMoveCounter() == 1) {
                swapPossible.scalars().forEach(b -> b.setBoolean(true));
            }
        }
    }

    public Dataset createDataSet(GameState gameState, Output output) {
        return null;
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

    @Override
    public void run() {
        final List<Task> tasks = new ArrayList<>();
        final TBool inputBuffer = Tensor.of(TBool.class, Shape.of(BATCH_SIZE, NUM_INPUT_CHANNELS, BOARD_SIZE * BOARD_SIZE));
        while (true) {
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
            extractFeatures(tasks.stream().map(Task::gameState).toList(), inputBuffer);
            try (
                    Result result = session.runner()
                            .feed("input", inputBuffer)
                            .addTarget("policyOut")
                            .addTarget("valueOut")
                            .run()
            ) {
                TFloat32 policyOut = (TFloat32) result.get("policyOut").orElseThrow();
                TFloat32 valueOut = (TFloat32) result.get("valueOut").orElseThrow();
                for (int i = 0; i < tasks.size(); i++) {
                    Task task = tasks.get(i);
                    float[] policyJvm = new float[BOARD_SIZE * BOARD_SIZE];
                    for (int flat = 0; flat < BOARD_SIZE * BOARD_SIZE; flat++) {
                        policyJvm[flat] = policyOut.getFloat(i, equalizeIndex(flat, task.gameState.getSideToMove()));
                    }
                    Output output = new Output(
                            policyJvm,
                            valueOut.getFloat(i)
                    );
                    cache.put(task.gameState, output);
                    task.future.complete(output);
                }
            }
            tasks.clear();
        }
    }

    @Override
    public void close() {
        session.close();
        graph.close();
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
