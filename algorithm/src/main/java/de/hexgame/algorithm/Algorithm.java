package de.hexgame.algorithm;

import de.hexgame.logic.*;
import de.hexgame.logic.GameState;

import java.util.List;

public class Algorithm {

    private final int board;


    public Algorithm() {
        board = GameState.BOARD_SIZE;
    }

    public Position bestPosition(GameState gameState) {
        Position bestPosition = null;
        double bestRating = Double.NEGATIVE_INFINITY;
        double calcRating;
        List<Move> legalMoves = gameState.getLegalMoves();


        for (Move move: legalMoves) {
            Position position = new Position(move.targetHexagon().getIndex());
            calcRating = calculatePieceRating(position, gameState.getSideToMove(), gameState);

            //Checking if possible position is better than the best position
            if (calcRating > bestRating) {
                bestRating = calcRating;
                bestPosition = position;
            }
        }
        return bestPosition;
    }

    public double calculateRating(Position position, Piece.Color usedColor, GameState gameState) {
        double tempRating = 0;

        if (!position.isValid()) {
            return tempRating;
        }

        Piece tempPiece = gameState.getPiece(position);

        if (tempPiece == null) {
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

    public double calculatePieceRating(Position position, Piece.Color usedColor, GameState gameState) {
        double rating = 0;
        Position tempPosition;

        for (Direction direction: Direction.ALL) {
            tempPosition = position.add(direction);
            rating = rating + calculateRating(tempPosition, usedColor, gameState);
        }
        return rating;
    }

    public Position bestPositionIn2(GameState gameState) {
        Position bestPostion = null;
        Position tempPosition;
        Position tempOtherColorPosition;
        Position tempDirectionPosition;
        Position tempOtherColorDirectionPosition;
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
        Piece.Color usedColor = gameState.getSideToMove();

        if (usedColor == Piece.Color.BLUE) {
            otherColor = Piece.Color.RED;
        } else {
            otherColor = Piece.Color.BLUE;
        }

        for (Move move : gameState.getLegalMoves()) {
            tempPosition = new Position(move.targetHexagon().getIndex());
            dummyPiece = new Piece(usedColor);


            for (Direction direction : Direction.ALL) {
                tempDirectionPosition = tempPosition.add(direction);
                tempRating = tempRating + calculateRating(tempDirectionPosition, usedColor, gameState);
                if (!tempDirectionPosition.isValid() || gameState.getPiece(tempDirectionPosition) == null) {
                    continue;
                }
                if (gameState.getPiece(tempDirectionPosition).isConnectedLow()) {
                    dummyPiece.setConnectedLow(true);
                }
                if (gameState.getPiece(tempDirectionPosition).isConnectedHigh()) {
                    dummyPiece.setConnectedHigh(true);
                }
            }

            for (Move move1 : gameState.getLegalMoves()) {

                if (move1.targetHexagon().getIndex() == tempPosition.getIndex()) {
                    counterFreePieces--;
                    continue;
                }
                tempOtherColorPosition = new Position(move1.targetHexagon().getIndex());
                dummyOtherColorPiece = new Piece(otherColor);

                for (Direction otherColorDirection : Direction.ALL) {
                    tempOtherColorDirectionPosition = tempOtherColorPosition.add(otherColorDirection);
                    tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPosition, otherColor, gameState);
                    if (!tempOtherColorDirectionPosition.isValid() || gameState.getPiece(tempOtherColorDirectionPosition) == null) {
                        continue;
                    }
                    if (gameState.getPiece(tempOtherColorDirectionPosition).isConnectedLow()) {
                        dummyOtherColorPiece.setConnectedLow(true);
                    }
                    if (gameState.getPiece(tempOtherColorDirectionPosition).isConnectedHigh()) {
                        dummyOtherColorPiece.setConnectedHigh(true);
                    }
                }

                for (Move move2: gameState.getLegalMoves()) {
                    if (move2.targetHexagon().getIndex() == tempPosition.getIndex() || move2.targetHexagon().getIndex() == tempOtherColorPosition.getIndex()) {
                        counterFreePiecesSameColor--;
                        continue;
                    }
                    tempSameColorPosition = new Position(move2.targetHexagon().getIndex());

                    tempSameColorRating = calculatePieceRating(tempSameColorPosition, usedColor, gameState);
                }
            }
            tempRating = tempRating - (tempOtherColorRating / counterFreePieces) + (tempSameColorRating / (counterFreePiecesSameColor * counterFreePieces));

            if (tempRating > bestRating) {
                bestRating = tempRating;
                bestPostion = tempPosition;
            }
        }

        for (Move move : gameState.getLegalMoves()) {
            gameState.resetOnePiece(move.targetHexagon().getIndex());
        }
        return bestPostion;
    }

}