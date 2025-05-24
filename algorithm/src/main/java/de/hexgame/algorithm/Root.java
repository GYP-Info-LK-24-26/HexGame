package de.hexgame.algorithm;

import de.hexgame.logic.Direction;
import de.hexgame.logic.GameState;
import de.hexgame.logic.Piece;
import de.hexgame.logic.Position;
import lombok.Getter;


public class Root {
    public enum Acolor {
        RED,
        BLUE
    }

    private Piece[][] pieces;
    @Getter
    private static Acolor color;
    private GameState gameState;
    private int board;


    public Root(Acolor acolor) {
        color = acolor;
        gameState = new GameState();
        board = GameState.BOARD_SIZE;
    }

    public void addpossibleNodes() {
        Position position;

        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                position = new Position(i, j);
                if (gameState.getPiece(position).getColor() == null) {
                    pieces[i][j] = gameState.getPiece(position);
                }
            }
        }
    }

    public void clear() {
        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                pieces[i][j] = null;
            }
        }
    }

    public Position bestPiece() {
        Position bestPosition = null;
        double bestRating = -2147483648;
        double calcRating = 0;

        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                if (pieces[i][j] == null){
                    continue;
                }
                Position position = new Position(i, j);
                for (Direction direction : Direction.ALL) {
                    Position tempPosition = position.add(direction);
                    double tempRating = 0;
                    if (!tempPosition.isValid()) {
                        continue;
                    }
                    if (gameState.getPiece(tempPosition).getColor().equals(null)) {
                        tempRating++;
                    } else if (gameState.getPiece(tempPosition).getColor().equals(color)) {
                        if (gameState.getPiece(tempPosition).isConnectedHigh() || gameState.getPiece(tempPosition).isConnectedLow()) {
                            tempRating = tempRating + 20;
                        }
                        else {
                            tempRating = tempRating + 10;
                        }
                    }
                    else {
                        if (gameState.getPiece(tempPosition).isConnectedHigh() || gameState.getPiece(tempPosition).isConnectedLow()) {
                            tempRating = tempRating + 15;
                        }
                        else {
                            tempRating = tempRating -5;
                        }
                    }
                    tempRating = tempRating + Math.random() * 0.1;
                    calcRating = calcRating + tempRating;
                }
                if (calcRating > bestRating) {
                    bestRating = calcRating;
                    bestPosition = position;
                }
                calcRating = 0;
            }
        }
        return bestPosition;
    }
}