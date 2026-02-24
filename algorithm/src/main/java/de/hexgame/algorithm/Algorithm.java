package de.hexgame.algorithm;

import de.hexgame.logic.*;
import de.hexgame.logic.GameState;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static de.hexgame.logic.GameState.*;

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

    public double calculateRating(Position position, int usedColor) {
        double tempRating = 0;

        if (!position.isValid()) {
            return tempRating;
        }

        int tempPiece = gameState.getPiece(position);

        if (tempPiece == NO_PIECE) {
            tempRating++;
        } else if (tempPiece == usedColor) {
            if (true/*tempPiece.isConnectedHigh()*/ || true/*tempPiece.isConnectedLow()*/) {
                tempRating = tempRating + 15;
            } else {
                tempRating = tempRating + 5;
            }
        } else {
            if (true/*tempPiece.isConnectedHigh()*/ || true/*tempPiece.isConnectedLow()*/) {
                tempRating = tempRating + 10;
            } else {
                tempRating = tempRating - 1;
            }
        }

        tempRating = tempRating + Math.random();
        return tempRating;
    }

    public double calculatePieceRating(Position position, int usedColor) {
        double rating = 0;
        Position tempPosition;

        for (Direction direction: Direction.ALL) {
            tempPosition = position.add(direction);
            rating = rating + calculateRating(tempPosition, usedColor);
        }

        if (gameState.getSideToMove() == RED) {
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
        if (gameState.getSideToMove() == RED) {
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
                        && cGameState.getPiece(position.add(direction)) == NO_PIECE) {
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
                        && cGameState.getPiece(position.add(direction)) == NO_PIECE) {
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
        int usedColor = gameState.getSideToMove();
        int tempPiece;
        double bestLength = 0;
        List<Move> possibleMoves = new ArrayList<>();

        //Checks every possible move
        for (Move move : gameState.getLegalMoves()) {
            //Clones game to be abel to make moves
            cGameState = gameState.clone();
            boolean tempHighConnect = false, tempLowConnect = false;
            tempPosition = new Position(move.getIndex());

            //Checks if move is legal and the selected position is not occupied
            if (move.targetHexagon().isValid()
                    && cGameState.getPiece(move.targetHexagon()) == NO_PIECE) {

                //Simulates the move
                cGameState.makeMove(move);

                //If an ending position is possible, it will be selected
                if (cGameState.isFinished()) {
                    return move.targetHexagon();
                }

                //Checks the pieces in every direction
                for (Direction direction: Direction.ALL) {

                    //Checks if move is legal and the selected position is occupied
                    if (move.targetHexagon().add(direction).isValid()
                            && cGameState.getPiece(move.targetHexagon().add(direction)) != NO_PIECE) {

                        //generates a piece of the selected position
                        tempPiece = cGameState.getPiece(move.targetHexagon().add(direction));

                        if (true/*tempPiece.isConnectedHigh()*/ && tempPiece != gameState.getSideToMove()) {
                            tempHighConnect = true;
                        }
                        if (true/*tempPiece.isConnectedLow()*/ && tempPiece != gameState.getSideToMove()) {
                            tempLowConnect = true;
                        }
                        if (tempHighConnect && tempLowConnect) {
                            return move.targetHexagon();
                        }

                        //Checks if the next move of the opponent would be a loss for itself
                        if (gameState.getSideToMove() == BLUE) {
                            if (tempPiece == RED) {
                                if (true/*tempPiece.isConnectedHigh()*/
                                        && move.targetHexagon().column() == 0
                                        && move.targetHexagon().row() == move.targetHexagon().add(direction).row()) {
                                    return move.targetHexagon();
                                }
                                if (true/*tempPiece.isConnectedLow()*/
                                        && move.targetHexagon().column() == board - 1
                                        && move.targetHexagon().row() == move.targetHexagon().add(direction).row()) {
                                    return move.targetHexagon();
                                }
                                if (true/*tempPiece.isConnectedHigh()*/
                                        && move.targetHexagon().column() == 0
                                        && move.targetHexagon().row() == move.targetHexagon().add(direction).row() + 1) {
                                    return move.targetHexagon();
                                }
                                if (true/*tempPiece.isConnectedLow()*/
                                        && move.targetHexagon().column() == board - 1
                                        && move.targetHexagon().row() == move.targetHexagon().add(direction).row() - 1) {
                                    return move.targetHexagon();
                                }
                            }
                        }
                        else {
                            if (tempPiece == BLUE) {
                                if (true/*tempPiece.isConnectedHigh()*/
                                        && move.targetHexagon().row() == 0
                                        && move.targetHexagon().column() == move.targetHexagon().add(direction).column()) {
                                    return move.targetHexagon();
                                }
                                if (true/*tempPiece.isConnectedLow()*/
                                        && move.targetHexagon().row() == board - 1
                                        && move.targetHexagon().column() == move.targetHexagon().add(direction).column()) {
                                    return move.targetHexagon();
                                }
                                if (true/*tempPiece.isConnectedHigh()*/
                                        && move.targetHexagon().row() == 0
                                        && move.targetHexagon().column() == move.targetHexagon().add(direction).column() + 1) {
                                    return move.targetHexagon();
                                }
                                if (true/*tempPiece.isConnectedLow()*/
                                        && move.targetHexagon().row() == board - 1
                                        && move.targetHexagon().column() == move.targetHexagon().add(direction).column() - 1) {
                                    return move.targetHexagon();
                                }
                            }
                        }
                    }
                }
                //Calculates the rating of the piece
                moveRating[move.getIndex()] = (-0.01 * calculatePieceRating(tempPosition, usedColor)) + (countRow(cGameState, tempPosition) / cGameState.getHalfMoveCounter());

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
            cGameState = gameState.clone();
            cGameState.makeMove(move);

            for (Move move2 : cGameState.getLegalMoves()) {
                cGameState2 = cGameState.clone();
                boolean tempHighConnect = false, tempLowConnect = false;

                //Checks if selected position is legal and not occupied
                if (move.targetHexagon().isValid()
                        && cGameState2.getPiece(move.targetHexagon()) == NO_PIECE) {

                    //Makes move
                    cGameState2.makeMove(move2);

                    if (cGameState2.isFinished()) {
                        return move.targetHexagon();
                    }

                    //Checks the pieces in every direction
                    for (Direction direction : Direction.ALL) {
                        //Checks if the direction added to the position is legal and if there is a piece
                        if (move2.targetHexagon().add(direction).isValid()
                                && cGameState2.getPiece(move2.targetHexagon().add(direction)) != NO_PIECE) {

                            tempPiece = cGameState2.getPiece(move2.targetHexagon().add(direction));

                            //If it is possible the other color to finish the game, and it can be provided by this move, the selected position will be played
                            if (true/*tempPiece.isConnectedHigh()*/) {
                                tempHighConnect = true;
                            }
                            if (true/*tempPiece.isConnectedLow()*/) {
                                tempLowConnect = true;
                            }
                            if (tempLowConnect && tempHighConnect) {
                                return move.targetHexagon();
                            }
                            if (gameState.getSideToMove() == BLUE) {
                                if (tempPiece == RED) {
                                    if (true/*tempPiece.isConnectedHigh()*/
                                            && move.targetHexagon().column() == 0
                                            && move.targetHexagon().row() == move.targetHexagon().add(direction).row()) {
                                        return move.targetHexagon();
                                    }
                                    if (true/*tempPiece.isConnectedLow()*/
                                            && move.targetHexagon().column() == board - 1
                                            && move.targetHexagon().row() == move.targetHexagon().add(direction).row()) {
                                        return move.targetHexagon();
                                    }
                                    if (true/*tempPiece.isConnectedHigh()*/
                                            && move.targetHexagon().column() == 0
                                            && move.targetHexagon().row() == move.targetHexagon().add(direction).row() - 1) {
                                        return move.targetHexagon();
                                    }
                                    if (true/*tempPiece.isConnectedLow()*/
                                            && move.targetHexagon().column() == board - 1
                                            && move.targetHexagon().row() == move.targetHexagon().add(direction).row() + 1) {
                                        return move.targetHexagon();
                                    }
                                }
                            }
                            else {
                                if (tempPiece == BLUE) {
                                    if (true/*tempPiece.isConnectedHigh()*/
                                            && move.targetHexagon().row() == 0
                                            && move.targetHexagon().column() == move.targetHexagon().add(direction).column()) {
                                        return move.targetHexagon();
                                    }
                                    if (true/*tempPiece.isConnectedLow()*/
                                            && move.targetHexagon().row() == board - 1
                                            && move.targetHexagon().column() == move.targetHexagon().add(direction).column()) {
                                        return move.targetHexagon();
                                    }
                                    if (true/*tempPiece.isConnectedHigh()*/
                                            && move.targetHexagon().row() == 0
                                            && move.targetHexagon().column() == move.targetHexagon().add(direction).column() + 1) {
                                        return move.targetHexagon();
                                    }
                                    if (true/*tempPiece.isConnectedLow()*/
                                            && move.targetHexagon().row() == board - 1
                                            && move.targetHexagon().column() == move.targetHexagon().add(direction).column() - 1) {
                                        return move.targetHexagon();
                                    }
                                }
                            }
                        }
                    }
                    //Calculates the rating
                    moveRating[move.getIndex()] = moveRating[move.getIndex()] + (countRow(cGameState2, move2.targetHexagon()) / (cGameState2.getHalfMoveCounter() * (double) possibleMoves.size()));
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