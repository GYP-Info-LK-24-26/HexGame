package de.hexgame.ui;

import de.hexgame.logic.Position;

public class Util {

    public static Position convertToGameCords(double x, double y) {
        int yRow = (int) Math.round((44 - y) / 1.5);
        yRow = Math.max(yRow,0);
        int xRow = (int) Math.floor(x - (double) yRow / 2);
        return new Position(xRow, yRow);
    }
}
