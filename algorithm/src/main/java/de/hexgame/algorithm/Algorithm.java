package de.hexgame.algorithm;

import de.hexgame.logic.*;
import de.hexgame.logic.GameState;
import lombok.Getter;

import java.util.List;

public class Algorithm {

    //private Piece[][] pieces;
    private boolean[] piece;
    @Getter
    private final GameState gameState;
    private final int board;


    public Algorithm() {
        gameState = new GameState();
        board = GameState.BOARD_SIZE;
        piece = new boolean[board * board];
        for (int i = 0; i < (board * board); i++) {
            piece[i] = false;
        }
        //pieces = new Piece[board][board];
        //clear();
    }


    public void addPlacedNodes() {
        Position position;

        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                position = new Position(i, j);
                if (gameState.getPiece(position) != null) {
                    piece[i * board + j - 1] = true;
                }
            }
        }
    }


    public void addPiece(Position position) {
        piece[position.getIndex()] = true;
    }

    /*
    public void clear() {
        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                //pieces[i][j] = null;
            }
        }
    }
    */

    public Position bestPosition(Piece.Color usedColor) {
        Position bestPosition = null;
        double bestRating = Double.NEGATIVE_INFINITY;
        double calcRating = 0;
        List<Move> legalMoves = gameState.getLegalMoves();

        for (Move move: legalMoves) {
            if (!gameState.isLegalMove(move)) {
                for (int i = 0; i < 700; i++) {
                    i++;
                }
            }
        }
        //List<Position> legalPosition = gameState.getLegalPositions();
        Position position;

        for (Move move: legalMoves) {
            position = new Position(move.getIndex());
            calcRating = calculatePieceRating(position, usedColor);

            //Checking if possible position is better than the best position
            if ((calcRating > bestRating) && (gameState.getPiece(position) == null)) {
                bestRating = calcRating;
                bestPosition = position;
            }
        }

        return bestPosition;
    }

    public double calculateRating(Position position, Piece.Color usedColor) {
        double tempRating = 0;
        Piece tempPiece;

        if (!position.isValid()) {
            return tempRating;
        }

        tempPiece = gameState.getPiece(position);

        if (tempPiece == null) {
            tempRating++;
        } else if (tempPiece.getColor().equals(usedColor)) {
            if (tempPiece.isConnectedHigh() || tempPiece.isConnectedLow()) {
                tempRating = tempRating + 30;
            } else {
                tempRating = tempRating + 15;
            }
        } else {
            if (tempPiece.isConnectedHigh() || tempPiece.isConnectedLow()) {
                tempRating = tempRating + 15;
            } else {
                tempRating = tempRating - 8;
            }
        }

        tempRating = tempRating + (Math.random() * 0.1);
        return tempRating;
    }

    public double calculatePieceRating(Position position, Piece.Color usedColor) {
        double rating = 0;
        Position tempPosition;

        for (Direction direction: Direction.ALL) {
            tempPosition = position.add(direction);
            rating = rating + calculateRating(tempPosition, usedColor);
        }
        return rating;
    }

    public Position bestPositionIn2(Piece.Color usedColor) {
        Position bestPostion = null;
        Position tempPositon;
        Position tempOtherColorPosition;
        Position tempDirectionPostiton;
        Position tempOtherColorDirectionPostion;
        Position tempSameColorPosition;
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
                if (piece[i * board + j]) {
                    continue;
                }

                tempPositon = new Position(i, j);
                dummyPiece = gameState.getPiece(new Position(i, j));


                for (Direction direction: Direction.ALL){
                    tempDirectionPostiton = tempPositon.add(direction);
                    if (tempDirectionPostiton.isValid()) {
                        tempRating = tempRating + calculateRating(tempDirectionPostiton, usedColor);
                        if (gameState.getPiece(tempDirectionPostiton) != null) {
                            if (gameState.getPiece(tempDirectionPostiton).isConnectedLow()) {
                                dummyPiece.setConnectedLow(true);
                            }
                            if (gameState.getPiece(tempDirectionPostiton).isConnectedHigh()) {
                                dummyPiece.setConnectedHigh(true);
                            }
                        }
                    }
                }

                for (int k = 0; k < board; k++) {
                    for (int l = 0; l < board; l++) {
                        if (piece[k * board + l]) {
                            counterFreePieces--;
                            continue;
                        }
                        tempOtherColorPosition = new Position(k, l);
                        dummyOtherColorPiece = gameState.getPiece(new Position(k, l));

                        for (Direction otherColorDirection: Direction.ALL){
                            tempOtherColorDirectionPostion = tempOtherColorPosition.add(otherColorDirection);
                            if (tempOtherColorDirectionPostion.isValid()) {
                                tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPostion, otherColor);
                                if (gameState.getPiece(tempOtherColorDirectionPostion) != null) {
                                    if (gameState.getPiece(tempOtherColorDirectionPostion).isConnectedLow()) {
                                        dummyOtherColorPiece.setConnectedLow(true);
                                    }
                                    if (gameState.getPiece(tempOtherColorDirectionPostion).isConnectedHigh()) {
                                        dummyOtherColorPiece.setConnectedHigh(true);
                                    }
                                }
                            }
                        }

                        for (int m = 0; m < board; m++) {
                            for (int n = 0; n < board; n++) {
                                if (piece[m * board + n]) {
                                    counterFreePiecesSameColor--;
                                    continue;
                                }
                                tempSameColorPosition = new Position(m, n);

                                tempSameColorRating = calculatePieceRating(tempSameColorPosition, usedColor);
                            }
                        }
                        dummyOtherColorPiece = null;
                   }
                }
                tempRating = tempRating - (tempOtherColorRating / counterFreePieces) + (tempSameColorRating / (counterFreePiecesSameColor * counterFreePieces));

                if (tempRating > bestRating) {
                    bestRating = tempRating;
                    bestPostion = tempPositon;
                }
                dummyPiece = null;
            }
        }
        return bestPostion;
    }

}