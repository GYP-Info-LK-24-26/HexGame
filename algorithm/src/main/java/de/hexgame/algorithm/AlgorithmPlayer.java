package de.hexgame.algorithm;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;
import de.hexgame.logic.Position;


public class AlgorithmPlayer implements Player {

    private final Algorithm calculate;
    private final double movesToCalculate;
    private final String name;

    public AlgorithmPlayer(int movesToCalculate) {
        calculate = new Algorithm();
        this.movesToCalculate = movesToCalculate;
        name = "Algorithm Player";
    }

    public AlgorithmPlayer(double movesToCalculate, String name) {
        calculate = new Algorithm();
        this.movesToCalculate = movesToCalculate;
        this.name = name;
    }
    public AlgorithmPlayer() {
        calculate = new Algorithm();
        movesToCalculate = 6;
        name = "Algorithm Player";
    }


    public Position start(double movesToCalculate) {
        Position temp;
        temp = calculate.longRowAlgorithm();
        return temp;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Move think(GameState gameState) {
        calculate.setGameState(gameState);
        return new Move(start(movesToCalculate));
    }
}
