package de.hexgame.algorithm;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Position;

public class Run {

    private final Algorithm algorithm;

    public Run() {
        algorithm = new Algorithm();
    }

    public Position start(double movesToCalculate, GameState gameState) {
        Position temp;
        if (movesToCalculate == 1) {temp = algorithm.bestPosition(gameState);}
        else if (movesToCalculate == 2){temp = algorithm.bestPositionIn2(gameState);}
        else if (movesToCalculate == 2.5){temp = algorithm.bestPositionIn2AndAHalf(gameState);}
        else if (movesToCalculate == 3){temp = algorithm.bestPositionIn3(gameState);}
        else if (movesToCalculate == 4){temp = algorithm.betterAlgorithm(gameState);}
        else {temp = algorithm.betterAlgorithmIn2(gameState);}
        return temp;
    }
}
