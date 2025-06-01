package de.hexgame.algorithm;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;

public class AlgorithmPlayer implements Player {

    private final Run calculate;

    public AlgorithmPlayer() {
        calculate = new Run();
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Move think(GameState gameState) {
        return new Move(calculate.start(gameState.getSideToMove()));
    }
}
