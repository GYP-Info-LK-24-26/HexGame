package de.hexgame.algorithm;

import de.hexgame.logic.Direction;
import de.hexgame.logic.GameState;
import de.hexgame.logic.Piece;
import de.hexgame.logic.Position;
import lombok.Getter;


public class Root {

    private Piece[][] pieces;
    @Getter
    private static Piece.Color color;
    private final GameState gameState;
    private final int board;


    public Root(Piece[][] pieces, Piece.Color acolor) {
        this.pieces = pieces;
        color = acolor;
        gameState = new GameState();
        board = GameState.BOARD_SIZE;
    }

    public void addPossibleNodes() {
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

    public Position bestPosition(Piece.Color acolor) {
        Position bestPosition = null;
        double bestRating = -2147483648;
        double calcRating = 0;
        Piece.Color usedColor = acolor;

        //If not the color of the algorithm-side should be accumulated
        if (acolor == null) {
            usedColor = color;
        }

        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                Position position = new Position(i, j); //Possible postion
                if (pieces[i][j] == null){
                    continue;
                }

                for (Direction direction : Direction.ALL) { //Neighbouring Pieces to the possible position
                    Position tempPosition = position.add(direction);
                    Piece tempPiece = gameState.getPiece(tempPosition);
                    double tempRating = 0;

                    //Evaluating the usefulness of the neighbouring piece of the possible position
                    if (!tempPosition.isValid()) {
                        continue;
                    }

                    if (tempPiece.getColor().equals(null)) {
                        tempRating++;
                    } else if (tempPiece.getColor().equals(usedColor)) {
                        if (tempPiece.isConnectedHigh() || tempPiece.isConnectedLow()) {
                            tempRating = tempRating + 20;
                        }
                        else {
                            tempRating = tempRating + 10;
                        }
                    }
                    else {
                        if (tempPiece.isConnectedHigh() || tempPiece.isConnectedLow()) {
                            tempRating = tempRating + 15;
                        }
                        else {
                            tempRating = tempRating -5;
                        }
                    }
                    tempRating = tempRating + Math.random();
                    calcRating = calcRating + tempRating; //Adding the value of the neighbour to the value of the possible position
                }
                //Checking if possible position is better than the best position
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