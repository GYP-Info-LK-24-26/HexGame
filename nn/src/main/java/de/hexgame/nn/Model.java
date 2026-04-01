package de.hexgame.nn;

import de.hexgame.logic.GameState;
import lombok.extern.slf4j.Slf4j;
import org.tensorflow.*;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.types.TFloat32;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.hexgame.logic.GameState.*;

@Slf4j
public class Model implements Closeable {
    private static final int NUM_INPUT_CHANNELS = 5;

    private final SavedModelBundle bundle;
    private final SessionFunction servingFunction;

    public Model(File file) {
        bundle = SavedModelBundle.load(file.getAbsolutePath(), "serve");
        servingFunction = bundle.function("serving_default");
    }

    public List<Output> predict(List<GameState> gameStates) {
        int batchSize = gameStates.size();
        List<Output> results = new ArrayList<>(batchSize);

        try (TFloat32 inputBuffer = TFloat32.tensorOf(
                Shape.of(batchSize, NUM_INPUT_CHANNELS, BOARD_SIZE, BOARD_SIZE))) {
            extractFeatures(gameStates, inputBuffer);
            try (Result result = servingFunction.call(Map.of("input", inputBuffer))) {
                @SuppressWarnings("resource")
                TFloat32 policyOut = (TFloat32) result.get("policy").orElseThrow();
                @SuppressWarnings("resource")
                TFloat32 valueOut = (TFloat32) result.get("value").orElseThrow();
                for (int i = 0; i < batchSize; i++) {
                    GameState state = gameStates.get(i);
                    float[] policy = new float[BOARD_SIZE * BOARD_SIZE];
                    for (int flat = 0; flat < BOARD_SIZE * BOARD_SIZE; flat++) {
                        policy[flat] = policyOut.getFloat(i, equalizeIndex(flat, state.getSideToMove()));
                    }
                    results.add(new Output(policy, valueOut.getFloat(i, 0)));
                }
            }
        }

        return results;
    }

    private void extractFeatures(List<GameState> gameStates, FloatNdArray featuresOut) {
        featuresOut.scalars().forEach(f -> f.setFloat(0.0f));

        for (int i = 0; i < gameStates.size(); i++) {
            GameState gameState = gameStates.get(i);

            FloatNdArray ownPieces = featuresOut.get(i, 0);
            FloatNdArray enemyPieces = featuresOut.get(i, 1);
            FloatNdArray swapPossible = featuresOut.get(i, 2);
            FloatNdArray ownTargets = featuresOut.get(i, 3);
            FloatNdArray enemyTargets = featuresOut.get(i, 4);

            for (int hexIndex = 0; hexIndex < BOARD_SIZE * BOARD_SIZE; hexIndex++) {
                int column = hexIndex % BOARD_SIZE;
                if (column == 0 || column == BOARD_SIZE - 1) {
                    ownTargets.setFloat(1.0f, hexIndex / BOARD_SIZE, hexIndex % BOARD_SIZE);
                }

                int row = hexIndex / BOARD_SIZE;
                if (row == 0 || row == BOARD_SIZE - 1) {
                    enemyTargets.setFloat(1.0f, hexIndex / BOARD_SIZE, hexIndex % BOARD_SIZE);
                }

                int piece = gameState.getPiece(hexIndex);
                if (piece == NO_PIECE) continue;

                int eqIndex = equalizeIndex(hexIndex, gameState.getSideToMove());

                if (piece == gameState.getSideToMove()) {
                    ownPieces.setFloat(1.0f, eqIndex / BOARD_SIZE, eqIndex % BOARD_SIZE);
                } else {
                    enemyPieces.setFloat(1.0f, eqIndex / BOARD_SIZE, eqIndex % BOARD_SIZE);
                }
            }

            if (gameState.getHalfMoveCounter() == 1) {
                swapPossible.scalars().forEach(f -> f.setFloat(1.0f));
            }
        }
    }

    private int equalizeIndex(int index, int sideToMove) {
        final boolean redToMove = sideToMove == RED;

        int row = index / BOARD_SIZE;
        int col = index % BOARD_SIZE;

        if (redToMove) {
            return row * BOARD_SIZE + col;
        } else {
            return col * BOARD_SIZE + row;
        }
    }

    @Override
    public void close() {
        bundle.close();
    }

    public record Output(float[] policy, float value) {
    }
}
