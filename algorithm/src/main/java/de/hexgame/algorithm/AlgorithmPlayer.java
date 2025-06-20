package de.hexgame.algorithm;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;

public class AlgorithmPlayer implements Player {

    private final Run calculate;
    private final int movesToCalculate;

    public AlgorithmPlayer(int movesToCalculate) {
        calculate = new Run();
        this.movesToCalculate = movesToCalculate;
    }

    @Override
    public String getName() {
        return "Algorithm Player";
    }

    @Override
    public Move think(GameState gameState) {
        return new Move(calculate.start(movesToCalculate, gameState));
    }
}
