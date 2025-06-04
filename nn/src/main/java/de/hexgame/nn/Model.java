package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Piece;
import de.hexgame.logic.Position;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Map;
import java.util.WeakHashMap;

import static de.hexgame.logic.GameState.BOARD_SIZE;

@RequiredArgsConstructor
public class Model {
    private static final int NUM_INPUT_CHANNELS = 6;
    private static final int[] INPUT_SHAPE = new int[]{NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE};

    private final ComputationGraph computationGraph;
    private final Map<GameState, Output> cache = new WeakHashMap<>();

    public Output predict(GameState gameState) {
        Output cachedOutput = cache.get(gameState);
        if (cachedOutput != null) {
            return cachedOutput;
        }

        try (INDArray features = Nd4j.create(INPUT_SHAPE)) {
            extractFeatures(gameState, features);
            val outputMap = computationGraph.feedForward(features, false);
            val policyOut = outputMap.get("policyOut");
            Output output = new Output(policyOut.toFloatVector(), outputMap.get("valueOut").getFloat(0));
            cache.put(gameState, output);
            return output;
        }
    }

    private void extractFeatures(GameState gameState, INDArray featuresOut) {
        // Start ChatGPT
        // ── 0-based channel handles ────────────────────────────────────────────────
        INDArray ownPieces   = featuresOut.getRow(0);   // shape: [B, B]
        INDArray enemyPieces = featuresOut.getRow(1);   // shape: [B, B]
        INDArray swapPossible= featuresOut.getRow(2);   // shape: [B, B]  (all zeros by default)
        INDArray blueGoal    = featuresOut.getRow(3);   // shape: [B, B]
        INDArray redGoal     = featuresOut.getRow(4);   // shape: [B, B]
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
