package de.hexgame.ui;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Position;

public class Util {

    public static Position convertToGameCords(double x, double y) {
        x -= UIGameBoard.get().getLeftOffset();
        x /= UIGameBoard.get().getScale();
        y = (y - UIGameBoard.get().getTopOffset()) / UIGameBoard.get().getyScale();
        boolean tip = y - Math.floor(y) >= 0.65;

        int yRow = GameState.BOARD_SIZE - (int) Math.floor(y) - 1;
        int xRow = (int) Math.floor(x - (double) yRow / 2 + 0.3);
        double deltaY = y - Math.floor(y);
        double deltaX = (x - (double) yRow / 2 + 0.3) - Math.floor((x - (double) yRow / 2 + 0.3));
        double delta = deltaY - deltaX;
        double xVal = deltaX; //(deltaX - 0.5) / 0.5;
        double yVal = deltaY; //(0.35 - (deltaY - 0.65)) / 0.35;
        //((0.35 - (deltaY - 0.65)) <= (-0.5 + deltaX) * 2/3)
        if(tip){
            if(deltaX > 0.6 && yVal > -0.6*(xVal - 0.5) + 0.95){
                yRow--;
                xRow = (int) Math.floor(x - (double) yRow / 2 + 0.3);
            } else if(!(delta > 0 && delta < 0.65) && deltaX < 0.6){
                yRow--;
                xRow = (int) Math.floor(x - (double) yRow / 2 + 0.3);
            }
        }

        /*if((yRow & 0x1) == 1)xRow = (int) Math.floor(x - (double) yRow / 2 + 0.3);
        else {
            System.out.println(yRow + "," + (x - (double) yRow / 2 + 0.3));

        }*/
        return new Position(yRow, xRow);
    }
}
