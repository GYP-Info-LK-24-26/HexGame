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
import org.tensorflow.types.TBool;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.family.TType;

import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Function;

import static de.hexgame.logic.GameState.BOARD_SIZE;
import static org.tensorflow.framework.optimizers.Optimizer.VARIABLE_V2;

@Slf4j
public class Model extends Thread implements Closeable {
    private static final String PADDING_TYPE = "SAME";
    private static final String INPUT = "input";
    private static final String POLICY_OUT = "policyOut";
    private static final String VALUE_OUT = "valueOut";
    private static final String POLICY_LABELS = "policyLabels";
    private static final String VALUE_LABELS = "valueLabels";
    private static final String TRAIN = "train";
    private static final int CHANNELS = 64;
    private static final int NUM_INPUT_CHANNELS = 5;
    private static final int BATCH_SIZE = 512;
    private static final Shape INPUT_SHAPE = Shape.of(BATCH_SIZE, NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE);
    static long c = 0;
    static long lastPrintTime = 0;
    private final Graph graph;
    private final Session session;
    private final Map<GameState, Output> cache = Collections.synchronizedMap(new WeakHashMap<>());
    private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();

    @SneakyThrows
    public Model(File file) {
        setDaemon(true);
        try (SavedModelBundle modelBundle = SavedModelBundle.load(file.getAbsolutePath())) {
            graph = modelBundle.graph();
            session = modelBundle.session();
        }
    }

    public Model() {
        setDaemon(true);
        graph = new Graph();

        Ops tf = Ops.create(graph);

        Placeholder<TBool> input = tf.withName(INPUT).placeholder(TBool.class,
                Placeholder.shape(INPUT_SHAPE));
        Placeholder<TFloat32> policyLabels = tf.withName(POLICY_LABELS).placeholder(TFloat32.class,
                Placeholder.shape(Shape.of(BATCH_SIZE, BOARD_SIZE * BOARD_SIZE)));
        Placeholder<TFloat32> valueLabels = tf.withName(VALUE_LABELS).placeholder(TFloat32.class,
                Placeholder.shape(Shape.of(BATCH_SIZE)));

        List<Variable<TFloat32>> allWeights = new ArrayList<>();

        Operand<TFloat32> inputFloats = tf.dtypes.cast(input, TFloat32.class);
        OperandPair<TFloat32> operand = addConvBatchNormBlock(tf, new OperandPair<>(inputFloats, inputFloats), 3,
                CHANNELS, true, allWeights);
        for (int i = 0; i < 12; i++) {
            operand = addResidualBlock(tf, operand, allWeights);
        }

        OperandPair<TFloat32> policyRelu = addConvBatchNormBlock(tf, operand, 1, 2, true, allWeights);
        OperandPair<TFloat32> policyFlat = policyRelu.apply(o ->
                tf.reshape(o, tf.array(BATCH_SIZE, 2 * BOARD_SIZE * BOARD_SIZE)));
        Variable<TFloat32> policyFcWeights = tf.variable(tf.math.mul(tf.random
                        .truncatedNormal(tf.array(2 * BOARD_SIZE * BOARD_SIZE, BOARD_SIZE * BOARD_SIZE), TFloat32.class),
                tf.constant(0.1f)));
        allWeights.add(policyFcWeights);
        Variable<TFloat32> policyFcBiases = tf.variable(tf.zeros(tf.array(BOARD_SIZE * BOARD_SIZE), TFloat32.class));
        OperandPair<TFloat32> policyLogits = policyFlat.apply(o ->
                tf.math.add(tf.linalg.matMul(o, policyFcWeights), policyFcBiases));
        tf.withName(POLICY_OUT).nn.softmax(policyLogits.inferenceOperand);

        OperandPair<TFloat32> valueRelu = addConvBatchNormBlock(tf, operand, 1, 1, true, allWeights);
        OperandPair<TFloat32> valueFlat = valueRelu.apply(o ->
                tf.reshape(o, tf.array(BATCH_SIZE, BOARD_SIZE * BOARD_SIZE)));
        Variable<TFloat32> valueFcWeights = tf.variable(tf.math.mul(tf.random
                        .truncatedNormal(tf.array(BOARD_SIZE * BOARD_SIZE, 256), TFloat32.class),
                tf.constant(0.1f)));
        allWeights.add(valueFcWeights);
        Variable<TFloat32> valueFcBiases = tf.variable(tf.zeros(tf.array(256), TFloat32.class));
        OperandPair<TFloat32> valueFc = valueFlat.apply(o ->
                tf.math.add(tf.linalg.matMul(o, valueFcWeights), valueFcBiases));
        Variable<TFloat32> valueOutWeights = tf.variable(tf.math.mul(tf.random
                        .truncatedNormal(tf.array(256, 1), TFloat32.class),
                tf.constant(0.1f)));
        allWeights.add(valueOutWeights);
        Variable<TFloat32> valueOutBias = tf.variable(tf.zeros(tf.array(1), TFloat32.class));
        tf.withName(VALUE_OUT).math.tanh(tf.math.add(tf.linalg.matMul(valueFc.inferenceOperand, valueOutWeights), valueOutBias));
        Operand<TFloat32> valueOutTrain = tf.math.tanh(tf.math.add(tf.linalg.matMul(valueFc.trainingOperand, valueOutWeights), valueOutBias));

        Operand<TFloat32> mainLoss = tf.math.add(tf.nn.softmaxCrossEntropyWithLogits(policyLogits.trainingOperand, policyLabels).loss(),
                tf.math.mean(tf.math.square(tf.math.sub(valueOutTrain, valueLabels)), tf.constant(0)));

        List<Operand<TFloat32>> l2Terms = new ArrayList<>();
        for (Variable<TFloat32> weights : allWeights) {
            l2Terms.add(tf.nn.l2Loss(weights));
        }
        Operand<TFloat32> l2 = tf.constant(0.0f);
        for (Operand<TFloat32> l2Term : l2Terms) {
            l2 = tf.math.add(l2, l2Term);
        }
        Operand<TFloat32> l2Loss = tf.math.mul(l2, tf.constant(1e-4f));

        Operand<TFloat32> loss = tf.math.add(mainLoss, l2Loss);

        Optimizer optimizer = new Momentum(graph, 1e-2f, 0.9f);
        optimizer.applyGradients(computeGradients(loss), TRAIN);

        session = new Session(graph);
    }

    private <T extends TType> List<Optimizer.GradAndVar<?>> computeGradients(Operand<?> loss) {
        List<Operation> variables = new ArrayList<>();
        graph
                .operations()
                .forEachRemaining(
                        (Operation op) -> {
                            if (op.type().equals(VARIABLE_V2) && !op.name().startsWith("INFERENCE_")) {
                                variables.add(op);
                            }
                        });

        org.tensorflow.Output<?>[] variableOutputArray = new org.tensorflow.Output[variables.size()];
        for (int i = 0; i < variables.size(); i++) {
            // First output of a variable is it's output.
            variableOutputArray[i] = variables.get(i).output(0);
        }

        org.tensorflow.Output<?>[] gradients = graph.addGradients(loss.asOutput(), variableOutputArray);
        List<Optimizer.GradAndVar<? extends TType>> gradVarPairs = new ArrayList<>();

        for (int i = 0; i < variableOutputArray.length; i++) {
            @SuppressWarnings("unchecked")
            org.tensorflow.Output<T> typedGrad = (org.tensorflow.Output<T>) gradients[i];
            @SuppressWarnings("unchecked")
            org.tensorflow.Output<T> typedVar = (org.tensorflow.Output<T>) variableOutputArray[i];
            gradVarPairs.add(new Optimizer.GradAndVar<>(typedGrad, typedVar));
        }

        return gradVarPairs;
    }

    private OperandPair<TFloat32> addConvBatchNormBlock(Ops tf, OperandPair<TFloat32> input, int width, int channels,
                                                    boolean useActivation, List<Variable<TFloat32>> allWeights) {
        Variable<TFloat32> convWeights = tf.variable(tf.math.mul(tf.random
                        .truncatedNormal(tf.array(width, width, input.trainingOperand.shape().get(1), channels), TFloat32.class),
                tf.constant(0.1f)));
        allWeights.add(convWeights);
        OperandPair<TFloat32> conv = input.apply(operand -> tf.nn.conv2d(operand, convWeights,
                Arrays.asList(1L, 1L, 1L, 1L), PADDING_TYPE, Conv2d.dataFormat("NCHW")));

        Variable<TFloat32> mean = tf.withName("INFERENCE_MEAN")
                .variable(tf.zeros(tf.array(channels), TFloat32.class));
        Variable<TFloat32> variance = tf.withName("INFERENCE_VARIANCE")
                .variable(tf.ones(tf.array(channels), TFloat32.class));

        Variable<TFloat32> scale = tf.variable(tf.ones(tf.array(channels), TFloat32.class));
        Variable<TFloat32> offset = tf.variable(tf.zeros(tf.array(channels), TFloat32.class));

        FusedBatchNorm<TFloat32, TFloat32> batchNormTrain = tf.nn.fusedBatchNorm(conv.trainingOperand, scale, offset,
                tf.constant(new float[0]), tf.constant(new float[0]), FusedBatchNorm.isTraining(true),
                FusedBatchNorm.dataFormat("NCHW"));
        FusedBatchNorm<TFloat32, TFloat32> batchNormInf = tf.nn.fusedBatchNorm(conv.inferenceOperand, scale, offset,
                mean, variance, FusedBatchNorm.isTraining(false), FusedBatchNorm.dataFormat("NCHW"));

        OperandPair<TFloat32> batchNorm = new OperandPair<>(batchNormTrain.y(), batchNormInf.y());

        if (useActivation) {
            return batchNorm.apply(tf.nn::relu);
        } else {
            return batchNorm;
        }
    }

    private OperandPair<TFloat32> addResidualBlock(Ops tf, OperandPair<TFloat32> input, List<Variable<TFloat32>> allWeights) {
        OperandPair<TFloat32> firstBnOut = addConvBatchNormBlock(tf, input, 3, CHANNELS, true, allWeights);
        OperandPair<TFloat32> secondBnOut = addConvBatchNormBlock(tf, firstBnOut, 3, CHANNELS, false, allWeights);
        return input.apply(tf.math::add, secondBnOut).apply(tf.nn::relu);
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
        List<ExperienceBuffer.Sample> samples = new ArrayList<>(BATCH_SIZE);
        try (TBool input = TBool.tensorOf(INPUT_SHAPE);
             TFloat32 policyLabels = TFloat32.scalarOf(1); TFloat32 valueLabels = TFloat32.scalarOf(1)) {
            for (int i = 0; i < numBatches; i++) {
                experienceBuffer.sample(samples, BATCH_SIZE);
                extractFeatures(samples.stream().map(ExperienceBuffer.Sample::gameState).toList(), input);
                session.runner()
                        .feed(INPUT, input)
                        .feed(POLICY_LABELS, policyLabels)
                        .feed(VALUE_LABELS, valueLabels)
                        .addTarget(TRAIN)
                        .run()
                        .close();
                samples.clear();
            }
        }
    }

    @SneakyThrows
    public void save(File file) {
        SavedModelBundle.exporter(file.getAbsolutePath())
                .withFunction(new SessionFunction(Signature.builder().build(), session))
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
                    ownTargets.setBoolean(true, hexIndex / BOARD_SIZE, hexIndex % BOARD_SIZE);
                }

                int row = hexIndex / BOARD_SIZE;
                if (row == 0 || row == BOARD_SIZE - 1) {
                    enemyTargets.setBoolean(true, hexIndex / BOARD_SIZE, hexIndex % BOARD_SIZE);
                }

                Piece piece = gameState.getPiece(hexIndex);
                if (piece == null) continue;

                int eqIndex = equalizeIndex(hexIndex, gameState.getSideToMove());

                if (piece.getColor() == gameState.getSideToMove()) {
                    ownPieces.setBoolean(true, eqIndex / BOARD_SIZE, eqIndex % BOARD_SIZE);
                } else {
                    enemyPieces.setBoolean(true, eqIndex / BOARD_SIZE, eqIndex % BOARD_SIZE);
                }
            }

            if (gameState.getHalfMoveCounter() == 1) {
                swapPossible.scalars().forEach(b -> b.setBoolean(true));
            }
        }
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
        final TBool inputBuffer = TBool.tensorOf(INPUT_SHAPE);
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
                            .feed(INPUT, inputBuffer)
                            .fetch(POLICY_OUT)
                            .fetch(VALUE_OUT)
                            .run()
            ) {
                @SuppressWarnings("resource") TFloat32 policyOut = (TFloat32) result.get(POLICY_OUT).orElseThrow();
                @SuppressWarnings("resource") TFloat32 valueOut = (TFloat32) result.get(VALUE_OUT).orElseThrow();
                for (int i = 0; i < tasks.size(); i++) {
                    Task task = tasks.get(i);
                    float[] policyJvm = new float[BOARD_SIZE * BOARD_SIZE];
                    for (int flat = 0; flat < BOARD_SIZE * BOARD_SIZE; flat++) {
                        policyJvm[flat] = policyOut.getFloat(i, equalizeIndex(flat, task.gameState.getSideToMove()));
                    }
                    Output output = new Output(
                            policyJvm,
                            valueOut.getFloat(i, 0)
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

    public record Output(float[] policy, float value) implements Serializable, Cloneable {
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

    private record OperandPair<T extends TType>(Operand<T> trainingOperand, Operand<T> inferenceOperand) {
        public OperandPair<T> apply(Function<Operand<T>, Operand<T>> operation) {
            return new OperandPair<>(operation.apply(trainingOperand), operation.apply(inferenceOperand));
        }

        public OperandPair<T> apply(BiFunction<Operand<T>, Operand<T>, Operand<T>> operation, OperandPair<T> argument) {
            return new OperandPair<>(operation.apply(trainingOperand, argument.trainingOperand),
                    operation.apply(inferenceOperand, argument.inferenceOperand));
        }
    }
}
