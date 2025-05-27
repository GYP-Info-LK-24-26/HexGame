package de.hexgame.algorithm;

import de.hexgame.logic.Piece;
import de.hexgame.logic.Position;

public class Run {

    private Root root;

    public Position start() {
        Position temp;
        root.addPossibleNodes();
        temp = root.bestPosition(null);
        root.clear();
        return temp;
    }

    public Position start(Piece.Color acolor) {
        Position temp;
        root.addPossibleNodes();
        temp = root.bestPosition(acolor);
        root.clear();
        return temp;
    }
}
