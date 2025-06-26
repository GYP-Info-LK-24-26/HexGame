package de.hexgame.algorithm;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;


public class AlgorithmPlayer implements Player {

    private final Run calculate;
    private final double movesToCalculate;
    private final String name;

    public AlgorithmPlayer(int movesToCalculate) {
        calculate = new Run();
        this.movesToCalculate = movesToCalculate;
        name = "Algorithm Player";
    }

    public AlgorithmPlayer(double movesToCalculate, String name) {
        calculate = new Run();
        this.movesToCalculate = movesToCalculate;
        this.name = name;
    }

    public AlgorithmPlayer() {
        calculate = new Run();
        movesToCalculate = 5;
        name = "Algorithm Player";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Move think(GameState gameState) {
        return new Move(calculate.start(movesToCalculate, gameState));
    }
}
