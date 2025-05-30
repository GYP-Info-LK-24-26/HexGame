package de.hexgame.algorithm;

import de.hexgame.logic.Piece;
import de.hexgame.logic.Position;

public class Run {

    private final Algorithm algorithm;

    public Run() {
        algorithm = new Algorithm();
    }

    public Position start(Piece.Color acolor) {
        Position temp;
        algorithm.addPossibleNodes();
        temp = algorithm.bestPosition(acolor);
        algorithm.clear();
        return temp;
    }
}
