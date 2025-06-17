package de.hexgame.algorithm;

import de.hexgame.logic.Piece;
import de.hexgame.logic.Position;

public class Run {

    private final Algorithm algorithm;

    public Run() {
        algorithm = new Algorithm();
    }

    public Position start(Piece.Color acolor, int movesToCalculate) {
        Position temp;
        algorithm.addPossibleNodes();
        if (movesToCalculate == 1) {
            temp = algorithm.bestPosition(acolor);
        }
        else {
            temp = algorithm.bestPositionIn2(acolor);
        }
        algorithm.clear();
        return temp;
    }
}
