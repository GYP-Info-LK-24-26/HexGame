package de.hexgame.algorithm;

import de.hexgame.logic.*;
import de.hexgame.logic.GameState;
import lombok.Setter;

import java.util.List;

public class Algorithm{

    private final int board;
    @Setter
    private GameState gameState;
    private boolean[] isVisited;

    public Algorithm() {
        board = GameState.BOARD_SIZE;
        isVisited = new boolean[121];
        clear();
    }

    public void clear() {
        for (int i = 0; i < 121; i++) {
            isVisited[i] = false;
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

    public int countRow(Position position, Direction direction, GameState cGameState) {
        int counter = 1;
        isVisited[position.getIndex()] = true;
        for (Direction direction1 : Direction.ALL) {
            if (direction == direction1
                    || !position.add(direction1).isValid()
                    || cGameState.getPiece(position.add(direction1)) == null) {
                continue;
            }
            if (cGameState.getPiece(position.add(direction1)).getColor() == cGameState.getSideToMove()
                    || isVisited[position.add(direction1).getIndex()]) {
                continue;
            }
            counter = counter + countRow(position.add(direction1), direction1.changeDirection(), cGameState);
        }
        return counter;
    }

    public Position bestPosition() {
        Position bestPosition = null;
        double bestRating = Double.NEGATIVE_INFINITY;
        double calcRating;
        List<Move> legalMoves = gameState.getLegalMoves();


        for (Move move: legalMoves) {
            Position position = new Position(move.getIndex());
            calcRating = calculatePieceRating(position, gameState.getSideToMove());

            //Checking if possible position is better than the best position
            if (calcRating > bestRating) {
                bestRating = calcRating;
                bestPosition = position;
            }
        }
        return bestPosition;
    }

    public Position bestPositionIn2() {
        Position bestPostion = null, tempPosition, tempOtherColorPosition, tempDirectionPosition, tempOtherColorDirectionPosition, tempSameColorPosition;
        Piece dummyPiece, dummyOtherColorPiece;
        double bestRating = Double.NEGATIVE_INFINITY;
        double tempRating = 0;
        double tempOtherColorRating = 0;
        double tempSameColorRating = 0;
        int counterFreePieces = board * board;
        int counterFreePiecesSameColor = board * board;
        GameState CGameState = gameState.clone();
        Piece.Color otherColor;
        Piece.Color usedColor = CGameState.getSideToMove();

        if (usedColor == Piece.Color.BLUE) {
            otherColor = Piece.Color.RED;
        } else {
            otherColor = Piece.Color.BLUE;
        }

        for (Move move : CGameState.getLegalMoves()) {
            tempPosition = new Position(move.targetHexagon().getIndex());
            dummyPiece = new Piece(usedColor);
            //dummyPiece = CGameState.


            for (Direction direction : Direction.ALL) {
                tempDirectionPosition = tempPosition.add(direction);
                tempRating = tempRating + calculateRating(tempDirectionPosition, usedColor);
                if (!tempDirectionPosition.isValid() || CGameState.getPiece(tempDirectionPosition) == null) {
                    continue;
                }
                if (CGameState.getPiece(tempDirectionPosition).isConnectedLow()) {
                    dummyPiece.setConnectedLow(true);
                }
                if (CGameState.getPiece(tempDirectionPosition).isConnectedHigh()) {
                    dummyPiece.setConnectedHigh(true);
                }
            }

            for (Move move1 : CGameState.getLegalMoves()) {

                if (move1.targetHexagon().getIndex() == tempPosition.getIndex()) {
                    counterFreePieces--;
                    continue;
                }
                tempOtherColorPosition = new Position(move1.targetHexagon().getIndex());
                dummyOtherColorPiece = new Piece(otherColor);

                for (Direction otherColorDirection : Direction.ALL) {
                    tempOtherColorDirectionPosition = tempOtherColorPosition.add(otherColorDirection);
                    tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPosition, otherColor);
                    if (!tempOtherColorDirectionPosition.isValid() || CGameState.getPiece(tempOtherColorDirectionPosition) == null) {
                        continue;
                    }
                    if (CGameState.getPiece(tempOtherColorDirectionPosition).isConnectedLow()) {
                        dummyOtherColorPiece.setConnectedLow(true);
                    }
                    if (CGameState.getPiece(tempOtherColorDirectionPosition).isConnectedHigh()) {
                        dummyOtherColorPiece.setConnectedHigh(true);
                    }
                }

                for (Move move2: CGameState.getLegalMoves()) {
                    if (move2.targetHexagon().getIndex() == tempPosition.getIndex() || move2.targetHexagon().getIndex() == tempOtherColorPosition.getIndex()) {
                        counterFreePiecesSameColor--;
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

    public Position bestPositionIn2AndAHalf() {
        Position bestPosition = null, tempPosition, tempDirectionPosition, tempOtherColorPosition, tempOtherColorDirectionPosition, tempSameColorPosition, tempSameColorDirectionPosition, tempOtherColorPosition2;
        Piece dummyPiece, dummyOtherColorPiece, dummySameColorPiece;
        double bestRating = Double.NEGATIVE_INFINITY;
        double tempRating = 0;
        double tempOtherColorRating = 0;
        double tempSameColorRating = 0;
        double tempOtherColorRating2 = 0;
        int counterFreePieces = board * board;
        int counterFreePiecesSameColor = board * board;
        int counterFreePiecesOtherColor = board * board;
        GameState CGameState = gameState.clone();
        Piece.Color otherColor;
        Piece.Color usedColor = CGameState.getSideToMove();

        if (usedColor == Piece.Color.BLUE) {
            otherColor = Piece.Color.RED;
        } else {
            otherColor = Piece.Color.BLUE;
        }

        for (Move move : CGameState.getLegalMoves()) {
            tempPosition = new Position(move.targetHexagon().getIndex());
            dummyPiece = new Piece(usedColor);

            for (Direction direction : Direction.ALL) {
                tempDirectionPosition = tempPosition.add(direction);
                tempRating = tempRating + calculateRating(tempDirectionPosition, usedColor);
                if (!tempDirectionPosition.isValid() || CGameState.getPiece(tempDirectionPosition) == null) {
                    continue;
                }
                if (CGameState.getPiece(tempDirectionPosition).isConnectedLow()) {
                    dummyPiece.setConnectedLow(true);
                }
                if (CGameState.getPiece(tempDirectionPosition).isConnectedHigh()) {
                    dummyPiece.setConnectedHigh(true);
                }
            }

            for (Move move1 : CGameState.getLegalMoves()) {
                if (move1.getIndex() == tempPosition.getIndex()) {
                    counterFreePieces--;
                    continue;
                }
                tempOtherColorPosition = new Position(move1.getIndex());
                dummyOtherColorPiece = new Piece(otherColor);

                for (Direction direction : Direction.ALL) {
                    tempOtherColorDirectionPosition = tempOtherColorPosition.add(direction);
                    tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPosition, otherColor);
                    if (!tempOtherColorDirectionPosition.isValid() || CGameState.getPiece(tempOtherColorDirectionPosition) == null) {
                        continue;
                    }
                    if (CGameState.getPiece(tempOtherColorDirectionPosition).isConnectedLow()) {
                        dummyOtherColorPiece.setConnectedLow(true);
                    }
                    if (CGameState.getPiece(tempOtherColorDirectionPosition).isConnectedHigh()) {
                        dummyOtherColorPiece.setConnectedHigh(true);
                    }
                }

                for (Move move2: CGameState.getLegalMoves()) {
                    if (move2.getIndex() == tempPosition.getIndex() || move2.getIndex() == tempOtherColorPosition.getIndex()) {
                        counterFreePiecesSameColor--;
                        continue;
                    }
                    tempSameColorPosition = new Position(move2.getIndex());
                    dummySameColorPiece = new Piece(usedColor);

                    for (Direction direction : Direction.ALL) {
                        tempSameColorDirectionPosition = tempSameColorPosition.add(direction);
                        tempSameColorRating = tempSameColorRating + calculateRating(tempSameColorDirectionPosition, usedColor);
                        if (!tempSameColorDirectionPosition.isValid() || CGameState.getPiece(tempSameColorDirectionPosition) == null) {
                            continue;
                        }
                        if (CGameState.getPiece(tempSameColorDirectionPosition).isConnectedLow()) {
                            dummySameColorPiece.setConnectedLow(true);
                        }
                        if (CGameState.getPiece(tempSameColorDirectionPosition).isConnectedHigh()) {
                            dummySameColorPiece.setConnectedHigh(true);
                        }
                    }

                    for (Move move3 : CGameState.getLegalMoves()) {
                        if (move3.getIndex() == tempPosition.getIndex() || move3.getIndex() == tempOtherColorPosition.getIndex() || move3.getIndex() == tempSameColorPosition.getIndex()) {
                            counterFreePiecesOtherColor--;
                            continue;
                        }
                        tempOtherColorPosition2 = new Position(move3.getIndex());
                        tempOtherColorRating2 = tempOtherColorRating2 + calculatePieceRating(tempOtherColorPosition2, otherColor);
                    }
                }
            }
            tempRating = tempRating - (tempOtherColorRating / counterFreePieces) + (tempSameColorRating / (counterFreePiecesSameColor * counterFreePieces)) - (tempOtherColorRating2 / (counterFreePieces * counterFreePiecesSameColor * counterFreePiecesOtherColor));

            if (tempRating > bestRating) {
                bestRating = tempRating;
                bestPosition = tempPosition;
            }
        }
        return bestPosition;
    }

    public Position bestPositionIn3() {
        Position bestPostion = null, tempPosition, tempDirectionPosition, tempOtherColorPosition, tempOtherColorDirectionPosition, tempSameColorPosition, tempSameColorDirectionPosition, tempOtherColorPosition2, tempOtherColorDirectionPosition2, tempSameColorPosition2;
        Piece dummyPiece, dummyOtherColorPiece, dummySameColorPiece, dummyOtherColorPiece2;
        double bestRating = Double.NEGATIVE_INFINITY;
        double tempRating = 0;
        double tempOtherColorRating = 0;
        double tempSameColorRating = 0;
        double tempOtherColorRating2 = 0;
        double tempSameColorRating2 = 0;
        int counterFreePieces = board * board;
        int counterFreePiecesSameColor = board * board;
        int counterFreePiecesOtherColor = board * board;
        int counterFreePiecesSameColor2 = board * board;
        GameState CGameState = gameState.clone();
        Piece.Color otherColor;
        Piece.Color usedColor = CGameState.getSideToMove();

        if (usedColor == Piece.Color.BLUE) {
            otherColor = Piece.Color.RED;
        } else {
            otherColor = Piece.Color.BLUE;
        }

        for (Move move : CGameState.getLegalMoves()) {
            tempPosition = new Position(move.targetHexagon().getIndex());
            dummyPiece = new Piece(usedColor);

            for (Direction direction : Direction.ALL) {
                tempDirectionPosition = tempPosition.add(direction);
                tempRating = tempRating + calculateRating(tempDirectionPosition, usedColor);
                if (!tempDirectionPosition.isValid() || CGameState.getPiece(tempDirectionPosition) == null) {
                    continue;
                }
                if (CGameState.getPiece(tempDirectionPosition).isConnectedLow()) {
                    dummyPiece.setConnectedLow(true);
                }
                if (CGameState.getPiece(tempDirectionPosition).isConnectedHigh()) {
                    dummyPiece.setConnectedHigh(true);
                }
            }

            for (Move move1 : CGameState.getLegalMoves()) {
                if (move1.getIndex() == tempPosition.getIndex()) {
                    counterFreePieces--;
                    continue;
                }
                tempOtherColorPosition = new Position(move1.getIndex());
                dummyOtherColorPiece = new Piece(otherColor);

                for (Direction direction : Direction.ALL) {
                    tempOtherColorDirectionPosition = tempOtherColorPosition.add(direction);
                    tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPosition, otherColor);
                    if (!tempOtherColorDirectionPosition.isValid() || CGameState.getPiece(tempOtherColorDirectionPosition) == null) {
                        continue;
                    }
                    if (CGameState.getPiece(tempOtherColorDirectionPosition).isConnectedLow()) {
                        dummyOtherColorPiece.setConnectedLow(true);
                    }
                    if (CGameState.getPiece(tempOtherColorDirectionPosition).isConnectedHigh()) {
                        dummyOtherColorPiece.setConnectedHigh(true);
                    }
                }

                for (Move move2: CGameState.getLegalMoves()) {
                    if (move2.getIndex() == tempPosition.getIndex() || move2.getIndex() == tempOtherColorPosition.getIndex()) {
                        counterFreePiecesSameColor--;
                        continue;
                    }
                    tempSameColorPosition = new Position(move2.getIndex());
                    dummySameColorPiece = new Piece(usedColor);

                    for (Direction direction : Direction.ALL) {
                        tempSameColorDirectionPosition = tempSameColorPosition.add(direction);
                        tempSameColorRating = tempSameColorRating + calculateRating(tempSameColorDirectionPosition, usedColor);
                        if (!tempSameColorDirectionPosition.isValid() || CGameState.getPiece(tempSameColorDirectionPosition) == null) {
                            continue;
                        }
                        if (CGameState.getPiece(tempSameColorDirectionPosition).isConnectedLow()) {
                            dummySameColorPiece.setConnectedLow(true);
                        }
                        if (CGameState.getPiece(tempSameColorDirectionPosition).isConnectedHigh()) {
                            dummySameColorPiece.setConnectedHigh(true);
                        }
                    }

                    for (Move move3 : CGameState.getLegalMoves()) {
                        if (move3.getIndex() == tempPosition.getIndex() || move3.getIndex() == tempOtherColorPosition.getIndex() || move3.getIndex() == tempSameColorPosition.getIndex()) {
                            counterFreePiecesOtherColor--;
                            continue;
                        }
                        tempOtherColorPosition2 = new Position(move3.getIndex());
                        dummyOtherColorPiece2 = new Piece(otherColor);

                        for (Direction direction : Direction.ALL) {
                            tempOtherColorDirectionPosition2 = tempOtherColorPosition2.add(direction);
                            tempOtherColorRating2 = tempOtherColorRating2 + calculateRating(tempOtherColorDirectionPosition2, otherColor);
                            if (!tempOtherColorDirectionPosition2.isValid() || CGameState.getPiece(tempOtherColorDirectionPosition2) == null) {
                                continue;
                            }
                            if (CGameState.getPiece(tempOtherColorDirectionPosition2).isConnectedLow()) {
                                dummyOtherColorPiece2.setConnectedLow(true);
                            }
                            if (CGameState.getPiece(tempOtherColorDirectionPosition2).isConnectedHigh()) {
                                dummyOtherColorPiece2.setConnectedHigh(true);
                            }
                        }

                        for (Move move4 : CGameState.getLegalMoves()) {
                            if (move4.getIndex() == tempPosition.getIndex() || move4.getIndex() == tempOtherColorPosition.getIndex() || move4.getIndex() == tempSameColorPosition.getIndex() || move4.getIndex() == tempOtherColorPosition2.getIndex()) {
                                counterFreePiecesSameColor2--;
                                continue;
                            }
                            tempSameColorPosition2 = new Position(move4.getIndex());
                            tempSameColorRating2 = tempOtherColorRating2 + calculatePieceRating(tempSameColorPosition2, usedColor);
                        }
                    }
                }
            }
            tempRating = tempRating - (tempOtherColorRating / counterFreePieces) + (tempSameColorRating / (counterFreePiecesSameColor * counterFreePieces)) - (tempOtherColorRating2 / (counterFreePieces * counterFreePiecesSameColor * counterFreePiecesOtherColor)) + (tempSameColorRating2 / (counterFreePieces * counterFreePiecesSameColor * counterFreePiecesOtherColor * counterFreePiecesSameColor2));

            if (tempRating > bestRating) {
                bestRating = tempRating;
                bestPostion = tempPosition;
            }
        }
        return bestPostion;
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
        Piece dummyPiece, dummyOtherColorPiece;
        double bestRating = Double.NEGATIVE_INFINITY;
        double tempRating = 0;
        double tempOtherColorRating = 0;
        double tempSameColorRating = 0;
        int counterFreePieces = board * board;
        double counterFreePiecesSameColor = board * board;
        GameState cGameState;
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
            dummyPiece = cGameState.getPiece(tempPosition);

            if (dummyPiece.isConnectedLow() && dummyPiece.isConnectedHigh()) {
                return tempPosition;
            }


            for (Direction direction: Direction.ALL) {
                tempDirectionPosition = tempPosition.add(direction);
                if (move.targetHexagon().add(direction).isValid() && cGameState.getPiece(move.targetHexagon().add(direction)) != null && cGameState.getPiece(move.targetHexagon().add(direction)).getColor() == otherColor) {
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
                    if (dummyPiece.isConnectedHigh() && move.targetHexagon().column() == 0 && move.targetHexagon().row() == move.targetHexagon().add(direction).row() || dummyPiece.isConnectedLow() && move.targetHexagon().column() == board - 1 && move.targetHexagon().row() == move.targetHexagon().add(direction).row()) {
                        return move.targetHexagon();
                    }
                }
                else {
                    if (dummyPiece.isConnectedHigh() && move.targetHexagon().row() == 0 && move.targetHexagon().column() == move.targetHexagon().add(direction).column() || dummyPiece.isConnectedLow() && move.targetHexagon().row() == board - 1 && move.targetHexagon().column() == move.targetHexagon().add(direction).column()) {
                        return move.targetHexagon();
                    }
                }
                tempRating = tempRating + calculateRating(tempDirectionPosition, usedColor);
            }

            if (tempHighConnect && tempLowConnect) {
                return tempPosition;
            }

            for (Move move1 : cGameState.getLegalMoves()) {

                if (move1.targetHexagon().getIndex() == tempPosition.getIndex()) {
                    counterFreePieces--;
                    continue;
                }
                tempOtherColorPosition = new Position(move1.targetHexagon().getIndex());
                dummyOtherColorPiece = new Piece(otherColor);

                for (Direction otherColorDirection : Direction.ALL) {
                    tempOtherColorDirectionPosition = tempOtherColorPosition.add(otherColorDirection);
                    tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPosition, otherColor);
                    if (!tempOtherColorDirectionPosition.isValid() || cGameState.getPiece(tempOtherColorDirectionPosition) == null) {
                        continue;
                    }
                    if (cGameState.getPiece(tempOtherColorDirectionPosition).isConnectedLow()) {
                        dummyOtherColorPiece.setConnectedLow(true);
                    }
                    if (cGameState.getPiece(tempOtherColorDirectionPosition).isConnectedHigh()) {
                        dummyOtherColorPiece.setConnectedHigh(true);
                    }
                }

                for (Move move2: cGameState.getLegalMoves()) {
                    if (move2.targetHexagon().getIndex() == tempPosition.getIndex() || move2.targetHexagon().getIndex() == tempOtherColorPosition.getIndex()) {
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
        GameState cGameState = gameState.clone();
        Position bestPostition = null;
        Position tempPosition;
        Piece.Color usedColor = cGameState.getSideToMove();
        Piece.Color otherColor;
        Piece tempPiece;
        Move move;
        double bestLength = 0;
        double tempLength = 0;

        if (usedColor == Piece.Color.RED) {otherColor = Piece.Color.BLUE;}
        else {otherColor = Piece.Color.RED;}

        for (int i = 0; i < 121; i++) {
            cGameState = gameState.clone();
            boolean tempHighConnect = false, tempLowConnect = false;
            tempPosition = new Position(i);
            move = new Move(tempPosition);
            if (cGameState.getPiece(move.targetHexagon()) == null && move.targetHexagon().isValid()) {
                cGameState.makeMove(move);
                if (cGameState.isFinished()) {
                    return move.targetHexagon();
                }
                for (Direction direction: Direction.ALL) {
                    if (move.targetHexagon().add(direction).isValid()
                            && cGameState.getPiece(move.targetHexagon().add(direction)) != null
                            && cGameState.getPiece(move.targetHexagon().add(direction)).getColor() == otherColor) {

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
                            if (tempPiece.isConnectedHigh() && move.targetHexagon().column() == 0 && move.targetHexagon().row() == move.targetHexagon().add(direction).row()
                                    || tempPiece.isConnectedLow() && move.targetHexagon().column() == board - 1 && move.targetHexagon().row() == move.targetHexagon().add(direction).row()) {
                                return move.targetHexagon();
                            }
                        }
                        else {
                            if (tempPiece.isConnectedHigh() && move.targetHexagon().row() == 0 && move.targetHexagon().column() == move.targetHexagon().add(direction).column()
                                    || tempPiece.isConnectedLow() && move.targetHexagon().row() == board - 1 && move.targetHexagon().column() == move.targetHexagon().add(direction).column()) {
                                return move.targetHexagon();
                            }
                        }
                    }
                }
                if (tempHighConnect && tempLowConnect) {
                    return move.targetHexagon();
                }
                tempLength = calculatePieceRating(tempPosition, usedColor);

                if (cGameState.getPiece(tempPosition).getColor() != usedColor) {
                    tempLength = tempLength + countRow(tempPosition, null, cGameState);
                }
            }
            if (tempLength > bestLength) {
                bestLength = tempLength;
                bestPostition = tempPosition;
            }
            clear();
        }
        return bestPostition;
    }
}