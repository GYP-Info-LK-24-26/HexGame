package de.hexgame.ui;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Position;

public class Util {

    public static Position convertToGameCords(double x, double y) {
        double yDelta = UIGameBoard.get().getRightBounds().get(1) - UIGameBoard.get().getRightBounds().get(0);
        y = (y - UIGameBoard.get().getTopOffset()) / UIGameBoard.get().getyScale();
        int yRow = GameState.BOARD_SIZE - (int) Math.floor(y) - 1;

        int start = 0;
        double xCPY = x - yRow * UIGameBoard.get().getScale() / 2.0f;//1.8675
        while (UIGameBoard.get().getRightBounds().get(start) < xCPY)start++;
        start--;
        x -= UIGameBoard.get().getLeftOffset();
        x /= UIGameBoard.get().getScale();

        boolean tip = y - Math.floor(y) >= 0.65;
        if(tip)return new Position(-1,-1);

        return new Position(yRow, start);
    }
}
