package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Piece;
import de.hexgame.logic.Position;
import lombok.RequiredArgsConstructor;
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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import static de.hexgame.logic.GameState.BOARD_SIZE;

@RequiredArgsConstructor
public class Model {
    private static final int NUM_INPUT_CHANNELS = 6;
    private static final int[] INPUT_SHAPE = new int[]{NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE};

    private final ComputationGraph computationGraph;
    private final Map<GameState, Output> cache = Collections.synchronizedMap(new WeakHashMap<>());

    public Model() {
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

    public Output predict(GameState gameState) {
        Output cachedOutput = cache.get(gameState);
        if (cachedOutput != null) {
            return cachedOutput;
        }

        try (INDArray features = Nd4j.create(INPUT_SHAPE)) {
            extractFeatures(gameState, features);
            INDArray[] outputs = computationGraph.output(features.reshape(1, NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE));
            INDArray policyOut = outputs[0];
            INDArray valueOut = outputs[1];
            Output output = new Output(policyOut.ravel().toFloatVector(), valueOut.getFloat(0));
            cache.put(gameState, output);
            return output;
        }
    }

    private void extractFeatures(GameState gameState, INDArray featuresOut) {
        // Start ChatGPT
        // ── 0-based channel handles ────────────────────────────────────────────────
        INDArray ownPieces   = featuresOut.get(NDArrayIndex.point(0));   // shape: [B, B]
        INDArray enemyPieces = featuresOut.get(NDArrayIndex.point(1));   // shape: [B, B]
        INDArray swapPossible= featuresOut.get(NDArrayIndex.point(2));   // shape: [B, B]  (all zeros by default)
        INDArray blueGoal    = featuresOut.get(NDArrayIndex.point(3));   // shape: [B, B]
        INDArray redGoal     = featuresOut.get(NDArrayIndex.point(4));   // shape: [B, B]
        final boolean redToMove = (gameState.getSideToMove() == Piece.Color.RED);

        // ── 1. Stones ──────────────────────────────────────────────────────────────
        for (int flat = 0; flat < BOARD_SIZE * BOARD_SIZE; flat++) {
            Piece p = gameState.getPiece(new Position(flat));
            if (p == null) continue;

        /* canonicalise: rotate board 90 ° ccw when Blue is to move
           so the side-to-move always aims left↔right.                */
            int row = flat / BOARD_SIZE;
            int col = flat %  BOARD_SIZE;
            int canRow, canCol;

            if (redToMove) {
                canRow = row;
                canCol = col;
            } else {                            // Blue to move → rotate ccw
                canRow = BOARD_SIZE - 1 - col;
                canCol = row;
            }

            if (p.getColor() == gameState.getSideToMove()) {
                ownPieces  .putScalar(canRow, canCol, 1.0f);
            } else {
                enemyPieces.putScalar(canRow, canCol, 1.0f);
            }
        }

        // ── 2. Swap feature (only valid on ply-1 in Hex) ───────────────────────────
        if (gameState.getHalfMoveCounter() == 1) {
            swapPossible.addi(1.0f); // broadcast over the entire plane
        }

        // ── 3. Goal edge masks ─────────────────────────────────────────────────────
        // column vector (BOARD_SIZE × 1) → first & last rows = 1
        INDArray colMask = Nd4j.zeros(BOARD_SIZE, 1);
        colMask.putScalar(0,       0, 1.0f);
        colMask.putScalar(BOARD_SIZE - 1,   0, 1.0f);

        // row vector (1 × BOARD_SIZE) → first & last columns = 1
        INDArray rowMask = Nd4j.zeros(1, BOARD_SIZE);
        rowMask.putScalar(0, 0,       1.0f);
        rowMask.putScalar(0, BOARD_SIZE - 1,   1.0f);

        blueGoal.addiColumnVector(colMask); // top / bottom edges
        redGoal .addiRowVector   (rowMask); // left / right edges

        // Optional: release temporary NDArrays (only needed on workspaces / GPUs)
        colMask.close();
        rowMask.close();
        // End ChatGPT
    }

    public MultiDataSet createDataSet(GameState gameState, Output output) {
        INDArray features = Nd4j.create(INPUT_SHAPE);
        extractFeatures(gameState, features);
        INDArray policy = Nd4j.create(output.policy(), new int[] {BOARD_SIZE, BOARD_SIZE});
        INDArray value = Nd4j.createFromArray(output.value());
        INDArray mask = Nd4j.ones(BOARD_SIZE, BOARD_SIZE);

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (!gameState.isLegalMove(new Move(new Position(i, j)))) {
                    mask.putScalar(i, j, 0.0f);
                }
            }
        }

        return new MultiDataSet(
                new INDArray[] {features},
                new INDArray[] {policy, value},
                null,
                new INDArray[] {mask}
        );
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
}
