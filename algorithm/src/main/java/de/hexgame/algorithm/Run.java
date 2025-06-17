package de.hexgame.algorithm;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Position;

public class Run {

    private final Algorithm algorithm;

    public Run() {
        algorithm = new Algorithm();
    }

    public Position start(int movesToCalculate, GameState gameState) {
        Position temp;
        if (movesToCalculate == 1) {
            temp = algorithm.bestPosition(gameState);
        }
        else {
            temp = algorithm.bestPositionIn2(gameState);
        }
        return temp;
    }
}
