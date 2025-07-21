package de.hexgame.algorithm;

import de.hexgame.logic.*;
import de.hexgame.logic.GameState;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class Algorithm extends Thread{

    private final int board;
    @Setter
    private GameState gameState;
    private int[][] isVisited;
    private double[] moveRating;

    public Algorithm() {
        board = GameState.BOARD_SIZE;
        isVisited = new int[board][board];
        moveRating = new double[board * board];

        clear();
        for (int i = 0; i < 121; i++) {
            moveRating[i] = 0.0;
        }
    }

    //Resets the break-condition
    public void clear() {
        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                isVisited[i][j] = 0;
            }
        }
    }

    public double calculateRating(Position position, Piece.Color usedColor) {
        double tempRating = 0;

        if (!position.isValid()) {
            return tempRating;
        }

        Piece tempPiece = gameState.getPiece(position);

        if (tempPiece == null) {
            tempRating++;
        } else if (tempPiece.getColor().equals(usedColor)) {
            gameState.update(position);
            if (tempPiece.isConnectedHigh() || tempPiece.isConnectedLow()) {
                tempRating = tempRating + 15;
            } else {
                tempRating = tempRating + 5;
            }
        } else {
            gameState.update(position);
            if (tempPiece.isConnectedHigh() || tempPiece.isConnectedLow()) {
                tempRating = tempRating + 10;
            } else {
                tempRating = tempRating - 1;
            }
        }

        tempRating = tempRating + Math.random();
        return tempRating;
    }

    public double calculatePieceRating(Position position, Piece.Color usedColor) {
        double rating = 0;
        Position tempPosition;

        for (Direction direction: Direction.ALL) {
            tempPosition = position.add(direction);
            rating = rating + calculateRating(tempPosition, usedColor);
        }

        if (gameState.getSideToMove().equals(Piece.Color.RED)) {
            if (position.column() == 0 || position.column() == board - 1) {
                rating = rating + 5;
            }
        }else {
            if (position.row() == 0 || position.row() == board - 1) {
                rating = rating + 5;
            }
        }

        return rating;
    }

    public double countRow(GameState cGameState, Position position) {
        double counter = 0.0;
        int row = position.row();
        int column = position.column();
        if (gameState.getSideToMove() == Piece.Color.RED) {
            isVisited[row][column]++;
            for (Direction direction : Direction.ALL) {
                if (!position.add(direction).isValid()) {
                    continue;
                }
                if (isVisited[position.add(direction).row()][position.add(direction).column()] > 2) {
                    continue;
                }
                if (position.column() == board - 1) {
                    if (direction == Direction.RIGHT
                            || direction == Direction.DOWN_RIGHT
                            || direction == Direction.UP_RIGHT) {
                        continue;
                    }
                } else if (position.column() == 0) {
                    if (direction == Direction.LEFT
                            || direction == Direction.DOWN_LEFT
                            || direction == Direction.UP_LEFT) {
                        continue;
                    }
                }
                if (position.isValid()
                        && cGameState.getPiece(position.add(direction)) == null) {
                     counter += countRow(cGameState, position.add(direction));
                }
            }
            if (counter == 0) {
                counter = 1.0;
            }
        }
        else {
            isVisited[row][column]++;
            for (Direction direction : Direction.ALL) {
                if (!position.add(direction).isValid()) {
                    continue;
                }
                if (isVisited[position.add(direction).row()][position.add(direction).column()] > 2) {
                    continue;
                }
                if (position.row() == board - 1) {
                    if (direction == Direction.DOWN_RIGHT
                            || direction == Direction.DOWN_LEFT) {
                        continue;
                    }
                } else if (position.column() == 0) {
                    if (direction == Direction.UP_RIGHT
                            || direction == Direction.UP_LEFT) {
                        continue;
                    }
                }
                if (position.isValid()
                        && cGameState.getPiece(position.add(direction)) == null) {
                    counter += countRow(cGameState, position.add(direction));
                }
            }
            if (counter == 0) {
                counter = 1.0;
            }
        }
        return counter;
    }


    public Position longRowAlgorithm() {
        GameState cGameState, cGameState2;
        Position bestPostition = null;
        Position tempPosition = new Position(0);
        Piece.Color usedColor = gameState.getSideToMove();
        Piece tempPiece;
        double bestLength = 0;
        List<Move> possibleMoves = new ArrayList<>();

        //Checks every possible move
        for (Move move : gameState.getLegalMoves()) {
            //Clones game to be abel to make moves
            cGameState = gameState.cloneWithoutListeners();
            boolean tempHighConnect = false, tempLowConnect = false;
            tempPosition = new Position(move.getIndex());

            //Checks if move is legal and the selected position is not occupied
            if (move.targetHexagon().isValid()
                    && cGameState.getPiece(move.targetHexagon()) == null) {

                //Simulates the move
                cGameState.makeMove(move);
                cGameState.update(move.targetHexagon());

                //If an ending position is possible, it will be selected
                if (cGameState.isFinished()) {
                    return move.targetHexagon();
                }

                //Checks the pieces in every direction
                for (Direction direction: Direction.ALL) {

                    //Checks if move is legal and the selected position is occupied
                    if (move.targetHexagon().add(direction).isValid()
                            && cGameState.getPiece(move.targetHexagon().add(direction)) != null) {

                        //generates a piece of the selected position
                        tempPiece = cGameState.getPiece(move.targetHexagon().add(direction));

                        if (tempPiece.isConnectedHigh()) {
                            tempHighConnect = true;
                        }
                        if (tempPiece.isConnectedLow()) {
                            tempLowConnect = true;
                        }
                        if (tempHighConnect && tempLowConnect) {
                            return move.targetHexagon();
                        }

                        //Checks if the next move of the opponent would be a loss for itself
                        if (gameState.getSideToMove() == Piece.Color.BLUE) {
                            if (tempPiece.isConnectedHigh()
                                    && move.targetHexagon().column() == 0
                                    && move.targetHexagon().row() == move.targetHexagon().add(direction).row()
                                    || tempPiece.isConnectedLow()
                                    && move.targetHexagon().column() == board - 1
                                    && move.targetHexagon().row() == move.targetHexagon().add(direction).row()) {
                                return move.targetHexagon();
                            }
                        }
                        else {
                            if (tempPiece.isConnectedHigh()
                                    && move.targetHexagon().row() == 0
                                    && move.targetHexagon().column() == move.targetHexagon().add(direction).column()
                                    || tempPiece.isConnectedLow()
                                    && move.targetHexagon().row() == board - 1
                                    && move.targetHexagon().column() == move.targetHexagon().add(direction).column()) {
                                return move.targetHexagon();
                            }
                        }
                    }
                }
                //Calculates the rating of the piece
                moveRating[move.getIndex()] = -0.25 * calculatePieceRating(tempPosition, usedColor);

                if (true) {
                    moveRating[move.getIndex()] = moveRating[move.getIndex()] + (countRow(cGameState, tempPosition) / cGameState.getHalfMoveCounter());
                }

                //Selects the "best" Moves of the list
                if (possibleMoves.size() <= 20) {
                    possibleMoves.add(move);
                }
                else {
                    for (Move pMove : possibleMoves) {
                        if (moveRating[move.getIndex()] > moveRating[pMove.getIndex()]) {
                            possibleMoves.remove(pMove);
                            possibleMoves.add(move);
                            break;
                        }
                    }
                }
            }
            clear();
        }

        //Checks every for the best moves from before every possible move for the next half-move
        for (Move move : possibleMoves) {
            cGameState = gameState.cloneWithoutListeners();
            cGameState.makeMove(move);
            cGameState.update(move.targetHexagon());

            for (Move move2 : cGameState.getLegalMoves()) {
                cGameState2 = cGameState.cloneWithoutListeners();
                boolean tempHighConnect = false, tempLowConnect = false;

                //Checks if selected position is legal and not occupied
                if (move.targetHexagon().isValid()
                        && cGameState2.getPiece(move.targetHexagon()) == null) {

                    //Makes move
                    cGameState2.makeMove(move2);
                    cGameState2.update(move2.targetHexagon());

                    if (cGameState2.isFinished()) {
                        return move.targetHexagon();
                    }

                    //Checks the pieces in every direction
                    for (Direction direction : Direction.ALL) {
                        //Checks if the direction added to the position is legal and if there is a piece
                        if (move2.targetHexagon().add(direction).isValid()
                                && cGameState2.getPiece(move2.targetHexagon().add(direction)) != null) {

                            tempPiece = cGameState2.getPiece(move2.targetHexagon().add(direction));

                            //If it is possible the other color to finish the game, and it can be provided by this move, the selected position will be played
                            if (tempPiece.isConnectedHigh()) {
                                tempHighConnect = true;
                            }
                            if (tempPiece.isConnectedLow()) {
                                tempLowConnect = true;
                            }
                            if (tempLowConnect && tempHighConnect) {
                                return move.targetHexagon();
                            }
                            if (gameState.getSideToMove() == Piece.Color.BLUE) {
                                if (tempPiece.isConnectedHigh()
                                        && move2.targetHexagon().column() == 0
                                        && move2.targetHexagon().row() == move2.targetHexagon().add(direction).row()
                                            || tempPiece.isConnectedLow()
                                        && move2.targetHexagon().column() == board - 1
                                        && move2.targetHexagon().row() == move2.targetHexagon().add(direction).row()) {
                                    return move.targetHexagon();
                                }
                            }
                            else {
                                if (tempPiece.isConnectedHigh()
                                        && move2.targetHexagon().row() == 0
                                        && move2.targetHexagon().column() == move2.targetHexagon().add(direction).column()
                                            || tempPiece.isConnectedLow()
                                        && move2.targetHexagon().row() == board - 1
                                        && move2.targetHexagon().column() == move2.targetHexagon().add(direction).column()) {
                                    return move.targetHexagon();
                                }
                            }
                        }
                    }
                    //Calculates the rating
                    moveRating[move.getIndex()] = (-0.15 * calculatePieceRating(move2.targetHexagon(), usedColor) / (double) possibleMoves.size()) + (countRow(cGameState2, move2.targetHexagon()) / (cGameState2.getHalfMoveCounter() * (double) possibleMoves.size()));
                }
                clear();

                //Selects the best rating
                if (moveRating[move.getIndex()] > bestLength) {
                    bestLength = moveRating[move.getIndex()];
                    bestPostition = new Position(move.getIndex());
                }

            }
        }
        //Checks if there is a bestPosition
        if (bestPostition == null) {
            return tempPosition;
        }
        return bestPostition;
    }
}