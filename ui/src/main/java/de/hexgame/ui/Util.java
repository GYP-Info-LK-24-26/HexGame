package de.hexgame.ui;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Position;

public class Util {

    public static Position convertToGameCords(double x, double y) {
        x -= UIGameBoard.get().getLeftOffset();
        x /= UIGameBoard.get().getScale();
        int yRow = GameState.BOARD_SIZE - (int) Math.floor((y - UIGameBoard.get().getTopOffset()) / UIGameBoard.get().getyScale()) - 1;
        int xRow = (int) Math.floor(x - (double) yRow / 2);
        return new Position(yRow, xRow);
    }
}
