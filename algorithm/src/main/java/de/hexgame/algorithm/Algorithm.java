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
    private boolean[][] isVisited;

    public Algorithm() {
        board = GameState.BOARD_SIZE;
        isVisited = new boolean[board][board];
        clear();
    }

    public void clear() {
        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                isVisited[i][j] = false;
            }
        }
    }

    public GameState update(List<Move> moves) {
        GameState temp = gameState.clone();
        for (Move move : moves) {
            temp.makeMove(move);
            temp.update(move.targetHexagon());
            temp.switchSideToMove();
        }
        return temp;
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
            isVisited[row][column] = true;
            for (Direction direction : Direction.ALL) {
                if (!position.add(direction).isValid()) {
                    continue;
                }
                if (isVisited[position.add(direction).row()][position.add(direction).column()]) {
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
                if (!position.isValid()
                        && cGameState.getPiece(position.add(direction)) == null) {
                     counter += countRow(cGameState, position.add(direction));
                }
            }
            if (counter == 0) {
                counter = 1.0;
            }
        }
        else {
            isVisited[row][column] = true;
            for (Direction direction : Direction.ALL) {
                if (!position.add(direction).isValid()) {
                    continue;
                }
                if (isVisited[position.add(direction).row()][position.add(direction).column()]) {
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
                if (!position.isValid()
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


    public double directionRow(GameState cGameState, Move move, Direction direction, List<Move> moves) {
        if (move.targetHexagon().add(direction).isValid()
                && cGameState.getPiece(move.targetHexagon().add(direction)) == null) {
            move = new Move(move.targetHexagon().add(direction));
            moves.add(move);
            return rowUntilEnd(moves, move) + 1.0 + Math.random() * 0.1;
        }
        else if (move.targetHexagon().add(direction).isValid()
                    && cGameState.getPiece(move.targetHexagon().add(direction)).getColor() == cGameState.getSideToMove()) {
            return rowUntilEnd(moves, move) + Math.random() * 0.1;
        }
        else if (move.targetHexagon().row() == 1
                    || move.targetHexagon().row() == board - 2
                    || move.targetHexagon().column() == 1
                    || move.targetHexagon().column() == board - 2) {
            return 1.0;
        }
        return Double.POSITIVE_INFINITY;
    }

    public double rowUntilEnd(List<Move> moves, Move move) {
        GameState cGameState;
        double temp;
        double best = Double.POSITIVE_INFINITY;
        cGameState = update(moves);
        boolean highRow = false, lowRow = false, highColumn = false, lowColumn = false;
        int counter = 2;

        if (cGameState.isFinished()) {
            return 0;
        }

        if (move.targetHexagon().column() <= 5) {
            lowColumn = true;
        }
        if (move.targetHexagon().column() >= 5) {
            highColumn = true;
        }
        if (move.targetHexagon().row() <= 5) {
            lowRow = true;
        }
        if (move.targetHexagon().row() >= 5) {
            highRow = true;
        }

        if (gameState.getSideToMove() == Piece.Color.RED) {
            if (cGameState.getPiece(move.targetHexagon()) != null
                    && cGameState.getPiece(move.targetHexagon()).isConnectedLow()) {
                if (lowRow) {
                    temp = directionRow(cGameState, move, Direction.DOWN_RIGHT, moves);
                    if (temp < best) {
                        best = temp;
                    }
                    if (temp < (board - move.targetHexagon().add(Direction.DOWN_RIGHT).column()) + counter
                            || temp < move.targetHexagon().add(Direction.DOWN_RIGHT).column() + counter) {
                        return temp;
                    }
                }

                if (highRow) {
                    temp = directionRow(cGameState, move, Direction.UP_RIGHT, moves);
                    if (temp < best) {
                        best = temp;
                    }

                    if (temp < (board - move.targetHexagon().add(Direction.UP_RIGHT).column()) + counter
                            || temp + counter < move.targetHexagon().add(Direction.UP_RIGHT).column()) {
                        return temp;
                    }
                }

                temp = directionRow(cGameState, move, Direction.RIGHT, moves);
                if (temp < best) {
                    best = temp;
                }
                if (temp < (board - move.targetHexagon().add(Direction.RIGHT).column()) + counter
                        || temp < move.targetHexagon().add(Direction.RIGHT).column() + counter) {
                    return temp;
                }
            }
            else if (cGameState.getPiece(move.targetHexagon()) != null
                    && cGameState.getPiece(move.targetHexagon()).isConnectedHigh()) {
                if (lowRow) {
                    temp = directionRow(cGameState, move, Direction.DOWN_LEFT, moves);
                    if (temp < best) {
                        best = temp;
                    }

                    if (temp <  (board - move.targetHexagon().add(Direction.DOWN_LEFT).column()) + counter
                            || temp < move.targetHexagon().add(Direction.DOWN_LEFT).column() + counter) {
                        return temp;
                    }
                }
                if (highRow) {
                    temp = directionRow(cGameState, move, Direction.UP_LEFT, moves);
                    if (temp < best) {
                        best = temp;
                    }

                    if (temp <  (board - move.targetHexagon().add(Direction.UP_LEFT).column()) + counter
                            || temp < move.targetHexagon().add(Direction.UP_LEFT).column() + counter) {
                        return temp;
                    }
                }

                temp = directionRow(cGameState, move, Direction.LEFT, moves);
                if (temp < best) {
                    best = temp;
                }
                if (temp < (board - move.targetHexagon().add(Direction.LEFT).column()) + counter
                        || temp < move.targetHexagon().add(Direction.LEFT).column() + counter) {
                    return temp;
                }
            }
            else if (false){
                for (Direction direction : Direction.ALL) {
                    temp = directionRow(cGameState, move, direction, moves);
                    if (temp < best) {
                        best = temp;
                    }
                    if (temp < (board - move.targetHexagon().add(direction).column()) + counter
                            || temp < move.targetHexagon().add(direction).column() + counter) {
                        return temp;
                    }
                }
            }
        }
        else {
            if (gameState.getSideToMove() == Piece.Color.BLUE) {
                if (cGameState.getPiece(move.targetHexagon()) != null
                        && cGameState.getPiece(move.targetHexagon()).isConnectedLow()) {
                    temp = directionRow(cGameState, move, Direction.DOWN_LEFT, moves);
                    if (temp < best) {
                        best = temp;
                    }
                    if (temp < (board - move.targetHexagon().add(Direction.DOWN_LEFT).row()) + counter
                            || temp < move.targetHexagon().add(Direction.DOWN_LEFT).row() + counter) {
                        return temp;
                    }

                    temp = directionRow(cGameState, move, Direction.DOWN_RIGHT, moves);
                    if (temp < best) {
                        best = temp;
                    }
                    if (temp < (board - move.targetHexagon().add(Direction.DOWN_RIGHT).row()) + counter
                            || temp < move.targetHexagon().add(Direction.DOWN_RIGHT).row() + counter) {
                        return temp;
                    }
                }
                else if (cGameState.getPiece(move.targetHexagon()) != null
                        && cGameState.getPiece(move.targetHexagon()).isConnectedHigh()) {
                    temp = directionRow(cGameState, move, Direction.UP_LEFT, moves);
                    if (temp < best) {
                        best = temp;
                    }
                    if (temp < (board - move.targetHexagon().add(Direction.UP_LEFT).row()) + counter
                            || temp < move.targetHexagon().add(Direction.UP_LEFT).row() + counter) {
                        return temp;
                    }

                    temp = directionRow(cGameState, move, Direction.UP_RIGHT, moves);
                    if (temp < best) {
                        best = temp;
                    }
                    if (temp < (board - move.targetHexagon().add(Direction.UP_RIGHT).row()) + counter
                            || temp < move.targetHexagon().add(Direction.UP_RIGHT).row() + counter) {
                        return temp;
                    }
                }
                else if (false){
                    for (Direction direction : Direction.ALL) {
                        temp = directionRow(cGameState, move, direction, moves);
                        if (temp < best) {
                            best = temp;
                        }
                        if (temp < (board - move.targetHexagon().add(direction).column()) + counter
                                || temp < move.targetHexagon().add(direction).column() + counter) {
                            return temp;
                        }
                    }
                }
            }
        }
        return best;
    }

    public Position betterAlgorithm() {
        GameState cGameState = gameState.clone();
        Position bestPosition = null;
        Piece.Color usedColor = cGameState.getSideToMove();
        Piece.Color otherColor;
        double bestRating = Double.NEGATIVE_INFINITY;
        double tempRating;
        Piece tempPiece;

        if (usedColor == Piece.Color.RED) otherColor = Piece.Color.BLUE;
        else otherColor = Piece.Color.RED;

        for (Move move: cGameState.getLegalMoves()) {
            boolean tempHighConnect = false, tempLowConnect = false;
            if (cGameState.getPiece(move.targetHexagon()) == null && move.targetHexagon().isValid()) {
                cGameState.makeMove(move);
                if (cGameState.isFinished()) {
                    return move.targetHexagon();
                }
                for (Direction direction: Direction.ALL) {
                    if (move.targetHexagon().add(direction).isValid() && cGameState.getPiece(move.targetHexagon().add(direction)) != null && cGameState.getPiece(move.targetHexagon().add(direction)).getColor() == otherColor) {
                        tempPiece = cGameState.getPiece(move.targetHexagon().add(direction));
                        if (tempPiece.isConnectedHigh()) {
                            tempHighConnect = true;
                        }
                        if (tempPiece.isConnectedLow()) {
                            tempLowConnect = true;
                        }
                        if (cGameState.isFinished()) {
                            return move.targetHexagon();
                        }
                        if (otherColor == Piece.Color.RED) {
                            if (tempPiece.isConnectedHigh() && move.targetHexagon().column() == 0 && move.targetHexagon().row() == move.targetHexagon().add(direction).row() || tempPiece.isConnectedLow() && move.targetHexagon().column() == board - 1 && move.targetHexagon().row() == move.targetHexagon().add(direction).row()) {
                                return move.targetHexagon();
                            }
                        }
                        else {
                            if (tempPiece.isConnectedHigh() && move.targetHexagon().row() == 0 && move.targetHexagon().column() == move.targetHexagon().add(direction).column() || tempPiece.isConnectedLow() && move.targetHexagon().row() == board - 1 && move.targetHexagon().column() == move.targetHexagon().add(direction).column()) {
                                return move.targetHexagon();
                            }
                        }
                    }
                }
                if (tempHighConnect && tempLowConnect) {
                    return move.targetHexagon();
                }
            }
            tempRating = calculatePieceRating(move.targetHexagon(), usedColor);

            if (tempRating > bestRating) {
                bestRating = tempRating;
                bestPosition = move.targetHexagon();
            }
        }
        return bestPosition;
    }

    public Position betterAlgorithmIn2() {
        Position bestPostion = null, tempPosition, tempOtherColorPosition, tempDirectionPosition, tempOtherColorDirectionPosition, tempSameColorPosition;
        double bestRating = Double.NEGATIVE_INFINITY;
        double tempRating = 0;
        double tempOtherColorRating = 0;
        double tempSameColorRating = 0;
        int counterFreePieces = board * board;
        double counterFreePiecesSameColor = board * board;
        GameState cGameState, cGameState2;
        Piece.Color otherColor;
        Piece.Color usedColor = gameState.getSideToMove();

        if (usedColor == Piece.Color.BLUE) {
            otherColor = Piece.Color.RED;
        } else {
            otherColor = Piece.Color.BLUE;
        }

        for (Move move : gameState.getLegalMoves()) {
            cGameState = gameState.clone();
            boolean tempHighConnect = false, tempLowConnect = false;
            tempPosition = new Position(move.getIndex());
            cGameState.makeMove(move);
            cGameState.update(move.targetHexagon());

            if (cGameState.isFinished()) {
                return tempPosition;
            }

            for (Direction direction: Direction.ALL) {
                tempDirectionPosition = tempPosition.add(direction);
                if (move.targetHexagon().add(direction).isValid()
                        && cGameState.getPiece(move.targetHexagon().add(direction)) != null
                        && cGameState.getPiece(move.targetHexagon().add(direction)).getColor() == otherColor) {
                    if (cGameState.getPiece(move.targetHexagon().add(direction)).isConnectedHigh()) {
                        tempHighConnect = true;
                    }
                    if (cGameState.getPiece(move.targetHexagon().add(direction)).isConnectedLow()) {
                        tempLowConnect = true;
                    }
                }
                if (cGameState.isFinished()) {
                    return move.targetHexagon();
                }
                if (otherColor == Piece.Color.RED) {
                    if (cGameState.getPiece(move.targetHexagon()).isConnectedHigh()
                                && move.targetHexagon().column() == 0
                                && move.targetHexagon().row() == move.targetHexagon().add(direction).row()
                            || cGameState.getPiece(move.targetHexagon()).isConnectedLow()
                                && move.targetHexagon().column() == board - 1
                                && move.targetHexagon().row() == move.targetHexagon().add(direction).row()) {
                        return move.targetHexagon();
                    }
                }
                else {
                    if (cGameState.getPiece(move.targetHexagon()).isConnectedHigh()
                                && move.targetHexagon().row() == 0
                                && move.targetHexagon().column() == move.targetHexagon().add(direction).column()
                            || cGameState.getPiece(move.targetHexagon()).isConnectedLow()
                                && move.targetHexagon().row() == board - 1
                                && move.targetHexagon().column() == move.targetHexagon().add(direction).column()) {
                        return move.targetHexagon();
                    }
                }
                tempRating = tempRating + calculateRating(tempDirectionPosition, usedColor);
            }

            if (tempHighConnect && tempLowConnect) {
                return tempPosition;
            }

            for (Move move1 : cGameState.getLegalMoves()) {
                cGameState2 = gameState.clone();
                cGameState2.makeMove(move);
                cGameState2.update(move.targetHexagon());
                if (move1.targetHexagon().getIndex() == tempPosition.getIndex()) {
                    counterFreePieces--;
                    continue;
                }
                tempOtherColorPosition = new Position(move1.targetHexagon().getIndex());
                cGameState2.makeMove(move1);
                if (cGameState2.isFinished()) {
                    return move.targetHexagon();
                }

                for (Direction otherColorDirection : Direction.ALL) {
                    tempOtherColorDirectionPosition = tempOtherColorPosition.add(otherColorDirection);
                    tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPosition, otherColor);
                    if (!tempOtherColorDirectionPosition.isValid()
                            || cGameState2.getPiece(tempOtherColorDirectionPosition) == null) {
                        continue;
                    }
                    if (cGameState2.getPiece(tempOtherColorDirectionPosition).isConnectedLow()) {
                        cGameState2.getPiece(tempOtherColorDirectionPosition).setConnectedLow(true);
                    }
                    if (cGameState2.getPiece(tempOtherColorDirectionPosition).isConnectedHigh()) {
                        cGameState2.getPiece(tempOtherColorDirectionPosition).setConnectedHigh(true);
                    }
                }

                for (Move move2: cGameState2.getLegalMoves()) {
                    if (move2.targetHexagon().getIndex() == tempPosition.getIndex()
                            || move2.targetHexagon().getIndex() == tempOtherColorPosition.getIndex()) {
                        counterFreePiecesSameColor = counterFreePiecesSameColor - (1 / (double) (board * board));
                        continue;
                    }
                    tempSameColorPosition = new Position(move2.targetHexagon().getIndex());

                    tempSameColorRating = calculatePieceRating(tempSameColorPosition, usedColor);
                }
            }
            tempRating = tempRating - (tempOtherColorRating / counterFreePieces) + (tempSameColorRating / (counterFreePiecesSameColor * counterFreePieces));

            if (tempRating > bestRating) {
                bestRating = tempRating;
                bestPostion = tempPosition;
            }
        }

        return bestPostion;
    }

    public Position longRowAlgorithm() {
        GameState cGameState;
        Position bestPostition = null;
        Position tempPosition = new Position(0);
        Piece.Color usedColor = gameState.getSideToMove();
        Piece.Color otherColor;
        Piece tempPiece;
        Move move;
        double bestLength = 0;
        double tempLength = 0;

        if (usedColor == Piece.Color.RED) {otherColor = Piece.Color.BLUE;}
        else {otherColor = Piece.Color.RED;}

        for (int i = 0; i < 121; i++) {
            cGameState = gameState.cloneWithoutListeners();
            boolean tempHighConnect = false, tempLowConnect = false;
            tempPosition = new Position(i);
            move = new Move(tempPosition);

            if (move.targetHexagon().isValid()
                    && cGameState.getPiece(move.targetHexagon()) == null) {
                cGameState.switchSideToMove();
                cGameState.makeMove(move);
                cGameState.update(move.targetHexagon());

                if (cGameState.isFinished()) {
                    return move.targetHexagon();
                }

                for (Direction direction: Direction.ALL) {
                    if (move.targetHexagon().add(direction).isValid()
                            && cGameState.getPiece(move.targetHexagon().add(direction)) != null) {

                        tempPiece = cGameState.getPiece(move.targetHexagon().add(direction));

                        if (tempPiece.isConnectedHigh()) {
                            tempHighConnect = true;
                        }
                        if (tempPiece.isConnectedLow()) {
                            tempLowConnect = true;
                        }
                        if (otherColor == Piece.Color.RED) {
                            if (tempPiece.isConnectedHigh() && move.targetHexagon().column() == 0
                                        && move.targetHexagon().row() == move.targetHexagon().add(direction).row()
                                    || tempPiece.isConnectedLow() && move.targetHexagon().column() == board - 1
                                        && move.targetHexagon().row() == move.targetHexagon().add(direction).row()) {
                                return move.targetHexagon();
                            }
                        }
                        else {
                            if (tempPiece.isConnectedHigh() && move.targetHexagon().row() == 0
                                        && move.targetHexagon().column() == move.targetHexagon().add(direction).column()
                                    || tempPiece.isConnectedLow() && move.targetHexagon().row() == board - 1
                                        && move.targetHexagon().column() == move.targetHexagon().add(direction).column()) {
                                return move.targetHexagon();
                            }
                        }
                    }
                }
                if (tempHighConnect && tempLowConnect) {
                    return move.targetHexagon();
                }
                tempLength = -0.15 * calculatePieceRating(tempPosition, usedColor);

                if (cGameState.getPiece(tempPosition) != null && cGameState.getPiece(tempPosition).getColor() == otherColor) {
                    tempLength = tempLength + (countRow(cGameState, tempPosition) / cGameState.getHalfMoveCounter());
                }
            }
            if (tempLength > bestLength) {
                bestLength = tempLength;
                bestPostition = tempPosition;
            }
            clear();
        }
        if (bestPostition == null) {
            return tempPosition;
        }
        return bestPostition;
    }

    public Position bestPosition() {
        Position bestPosition = null;
        int bestLength = Integer.MAX_VALUE;
        int tempLength = 0;
        int neighboringPieces;
        GameState cGameState = gameState.cloneWithoutListeners();
        Piece.Color usedColor = gameState.getSideToMove(), otherColor = Piece.Color.RED;
        boolean topConnected, bottomConnected;
        int sameColorConnected;

        if (usedColor == Piece.Color.RED) {
            otherColor = Piece.Color.BLUE;
        }

        for (Move move : cGameState.getLegalMoves()) {
            topConnected = false;
            bottomConnected = false;
            sameColorConnected = 0;
            neighboringPieces = 0;
            for (Direction direction : Direction.ALL) {
                if (move.targetHexagon().add(direction).isValid()) {
                    if (gameState.getPiece(move.targetHexagon().add(direction)) != null) {
                        neighboringPieces ++;
                        if (gameState.getPiece(move.targetHexagon().add(direction)).getColor() == usedColor) {
                            sameColorConnected ++;
                        }
                    }
                    else {
                        continue;
                    }

                    if (gameState.getPiece(move.targetHexagon().add(direction)).getColor() == Piece.Color.RED && usedColor == Piece.Color.BLUE) {
                        if (gameState.getPiece(move.targetHexagon().add(direction)).isConnectedHigh()
                            && move.targetHexagon().add(direction).row() == move.targetHexagon().row()
                            && move.targetHexagon().column() == 0) {
                            return move.targetHexagon();
                        }
                        if (gameState.getPiece(move.targetHexagon().add(direction)).isConnectedLow()
                            && move.targetHexagon().add(direction).row() == move.targetHexagon().row()
                            && move.targetHexagon().column() == board - 1) {
                            return move.targetHexagon();
                        }
                    } else if (gameState.getPiece(move.targetHexagon().add(direction)).getColor() == Piece.Color.BLUE && usedColor == Piece.Color.RED) {
                        if (gameState.getPiece(move.targetHexagon().add(direction)).isConnectedHigh()
                            && move.targetHexagon().add(direction).column() == move.targetHexagon().column()
                            && move.targetHexagon().add(direction).row() == 0) {
                            return move.targetHexagon();
                        }
                        if (gameState.getPiece(move.targetHexagon().add(direction)).isConnectedLow()
                            && move.targetHexagon().add(direction).row() == move.targetHexagon().row()
                            && move.targetHexagon().row() == 0) {
                            return move.targetHexagon();
                        }
                    }
                }
                else {
                    neighboringPieces ++;
                }
            }
            if (neighboringPieces >= 5 || sameColorConnected >= 4) {
                continue;
            }
            cGameState.makeMove(move);
            cGameState.update(move.targetHexagon());
            if (cGameState.isFinished()) {
                return move.targetHexagon();
            }
        }
        return betterAlgorithm();
    }

    public Position test() {
        double temp;
        double best = Double.POSITIVE_INFINITY;
        Move bestMove = null;

        for (Move move : gameState.getLegalMoves()) {
            List<Move> temp2 = new ArrayList<>();
            temp2.add(move);
            temp = rowUntilEnd(temp2, move);
            if (temp < best) {
                bestMove = move;
                best = temp;
            }
        }

        return bestMove.targetHexagon();
    }
}