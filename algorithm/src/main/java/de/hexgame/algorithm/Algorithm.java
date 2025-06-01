package de.hexgame.algorithm;

import de.hexgame.logic.*;
import de.hexgame.logic.GameState;
import lombok.Getter;

public class Algorithm {

    private Piece[][] pieces;
    @Getter
    private final GameState gameState;
    private final int board;


    public Algorithm() {
        gameState = new GameState();
        board = GameState.BOARD_SIZE;
        clear();
    }

    public void addPossibleNodes() {
        Position position;

        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                position = new Position(i, j);
                if (gameState.getPiece(position) == null) {
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

    public Position bestPosition(Piece.Color usedColor) {
        Position bestPosition = null;
        double bestRating = Double.NEGATIVE_INFINITY;
        double calcRating = 0;


        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                if (pieces[i][j] == null){
                    continue;
                }
                Position position = new Position(i, j); //Possible postion

                for (Direction direction : Direction.ALL) { //Neighbouring Pieces to the possible position
                    Position tempPosition = position.add(direction);
                    calcRating = calculateRating(tempPosition, usedColor);
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

    public double calculateRating(Position position, Piece.Color usedColor) {
        double tempRating = Double.NEGATIVE_INFINITY;
        Piece tempPiece = gameState.getPiece(position);

        if (!position.isValid()) {
            return tempRating;
        }

        if (tempPiece.equals(null)) {
            tempRating++;
        } else if (tempPiece.getColor().equals(usedColor)) {
            if (tempPiece.isConnectedHigh() || tempPiece.isConnectedLow()) {
                tempRating = tempRating + 20;
            } else {
                tempRating = tempRating + 10;
            }
        } else {
            if (tempPiece.isConnectedHigh() || tempPiece.isConnectedLow()) {
                tempRating = tempRating + 15;
            } else {
                tempRating = tempRating - 5;
            }
        }

        tempRating = tempRating + Math.random();
        return tempRating;
    }

    public Position bestPositionIn2(Piece.Color usedColor) {
        Position bestPostion = null;
        Position tempPositon;
        Position tempOtherColorPosition;
        Position tempDirectionPostiton;
        Position tempOtherColorDirectionPostion;
        Position tempSameColorPosition;
        Position tempSameColorDirectionPostion;
        Piece dummyPiece;
        Piece dummyOtherColorPiece;
        double bestRating = Double.NEGATIVE_INFINITY;
        double tempRating = 0;
        double tempOtherColorRating = 0;
        double tempSameColorRating = 0;
        int counterFreePieces = board * board;
        int counterFreePiecesSameColor = board * board;
        Piece.Color otherColor;

        if (usedColor == Piece.Color.BLUE) {
            otherColor = Piece.Color.RED;
        }
        else {
            otherColor = Piece.Color.BLUE;
        }

        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                if (pieces[i][j] == null) {
                    continue;
                }
                tempPositon = new Position(i, j);
                dummyPiece = new Piece(usedColor);

                for (Direction direction: Direction.ALL){
                    tempDirectionPostiton = tempPositon.add(direction);
                    tempRating = tempRating + calculateRating(tempDirectionPostiton, usedColor);
                }

                for (int k = 0; k < board; k++) {
                    for (int l = 0; l < board; l++) {
                        if (pieces[k][l] == null || pieces[k][l] == dummyPiece) {
                            counterFreePieces--;
                            continue;
                        }
                        tempOtherColorPosition = new Position(k, l);
                        dummyOtherColorPiece = new Piece(otherColor);

                        for (Direction otherColorDirection: Direction.ALL){
                            tempOtherColorDirectionPostion = tempOtherColorPosition.add(otherColorDirection);
                            tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPostion, otherColor);
                        }

                        for (int m = 0; m < board; m++) {
                            for (int n = 0; n < board; n++) {
                                if (pieces[m][n] == null || pieces[m][n] == dummyPiece || pieces[m][n] == dummyOtherColorPiece) {
                                    counterFreePiecesSameColor--;
                                    continue;
                                }
                                tempSameColorPosition = new Position(m, n);

                                for (Direction sameColorDirection: Direction.ALL) {
                                    tempSameColorDirectionPostion = tempSameColorPosition.add(sameColorDirection);
                                    tempSameColorRating = tempSameColorRating + calculateRating(tempSameColorDirectionPostion, usedColor);
                                }
                            }
                        }
                    }
                }
                tempRating = tempRating + (tempOtherColorRating / counterFreePieces) + (tempSameColorRating / (counterFreePiecesSameColor * counterFreePieces));

                if (tempRating > bestRating) {
                    bestRating = tempRating;
                    bestPostion = tempPositon;
                }
            }
        }
        dummyPiece = null;
        dummyOtherColorPiece = null;
        return bestPostion;
    }

}