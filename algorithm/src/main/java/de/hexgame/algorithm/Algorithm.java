package de.hexgame.algorithm;

import de.hexgame.logic.*;
import de.hexgame.logic.GameState;

import java.util.List;

public class Algorithm extends Thread{

    private final int board;


    public Algorithm() {
        board = GameState.BOARD_SIZE;
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
            gameState.update(position);
            if (tempPiece.isConnectedHigh() || tempPiece.isConnectedLow()) {
                tempRating = tempRating + 20;
            } else {
                tempRating = tempRating + 10;
            }
        } else {
            gameState.update(position);
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

    public Position bestPosition(GameState gameState) {
        Position bestPosition = null;
        double bestRating = Double.NEGATIVE_INFINITY;
        double calcRating;
        List<Move> legalMoves = gameState.getLegalMoves();


        for (Move move: legalMoves) {
            Position position = new Position(move.getIndex());
            calcRating = calculatePieceRating(position, gameState.getSideToMove(), gameState);

            //Checking if possible position is better than the best position
            if (calcRating > bestRating) {
                bestRating = calcRating;
                bestPosition = position;
            }
        }
        return bestPosition;
    }

    public Position bestPositionIn2(GameState gameState) {
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
                tempRating = tempRating + calculateRating(tempDirectionPosition, usedColor, CGameState);
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
                    tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPosition, otherColor, CGameState);
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

                    tempSameColorRating = calculatePieceRating(tempSameColorPosition, usedColor, CGameState);
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

    public Position bestPositionIn2AndAHalf(GameState gameState) {
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
                tempRating = tempRating + calculateRating(tempDirectionPosition, usedColor, CGameState);
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
                    tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPosition, otherColor, CGameState);
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
                        tempSameColorRating = tempSameColorRating + calculateRating(tempSameColorDirectionPosition, usedColor, CGameState);
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
                        tempOtherColorRating2 = tempOtherColorRating2 + calculatePieceRating(tempOtherColorPosition2, otherColor, CGameState);
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

    public Position bestPositionIn3(GameState gameState) {
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
                tempRating = tempRating + calculateRating(tempDirectionPosition, usedColor, CGameState);
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
                    tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPosition, otherColor, CGameState);
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
                        tempSameColorRating = tempSameColorRating + calculateRating(tempSameColorDirectionPosition, usedColor, CGameState);
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
                            tempOtherColorRating2 = tempOtherColorRating2 + calculateRating(tempOtherColorDirectionPosition2, otherColor, CGameState);
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
                            tempSameColorRating2 = tempOtherColorRating2 + calculatePieceRating(tempSameColorPosition2, usedColor, CGameState);
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

    public Position betterAlgorithm(GameState gameState) {
        GameState cGameState = gameState.clone();
        Position bestPosition = null;
        Piece.Color usedColor = cGameState.getSideToMove();
        Piece.Color otherColor;
        double bestRating = Double.NEGATIVE_INFINITY;
        double tempRating = 0.0;
        Piece tempPiece;

        if (usedColor == Piece.Color.RED) otherColor = Piece.Color.BLUE;
        else otherColor = Piece.Color.RED;

        for (Move move: cGameState.getLegalMoves()) {
            boolean tempHighConnect = false, tempLowConnect = false;
            if (cGameState.getPiece(move.targetHexagon()) == null && move.targetHexagon().isValid()) {
                cGameState.setPiece(move.targetHexagon(), new Piece(usedColor));
                cGameState.update(move.targetHexagon());
                tempPiece = cGameState.getPiece(move.targetHexagon());
                if (tempPiece.isConnectedHigh() && tempPiece.isConnectedLow()) {
                    return move.targetHexagon();
                }
                for (Direction direction: Direction.ALL) {
                    if (move.targetHexagon().add(direction).isValid() && cGameState.getPiece(move.targetHexagon().add(direction)) != null && cGameState.getPiece(move.targetHexagon().add(direction)).getColor() == otherColor) {
                        if (cGameState.getPiece(move.targetHexagon().add(direction)).isConnectedHigh()) {
                            tempHighConnect = true;
                        }
                        if (cGameState.getPiece(move.targetHexagon().add(direction)).isConnectedLow()) {
                            tempLowConnect = true;
                        }
                        cGameState.reset();
                        cGameState = gameState.clone();
                        cGameState.setPiece(move.targetHexagon(), new Piece(otherColor));
                        cGameState.update(move.targetHexagon());
                        if (cGameState.isFinished()) {
                            return move.targetHexagon();
                        }
                    }
                }
                if (tempHighConnect && tempLowConnect) {
                    return move.targetHexagon();
                }
            }
            tempRating = calculatePieceRating(move.targetHexagon(), usedColor, cGameState);

            if (tempRating > bestRating) {
                bestRating = tempRating;
                bestPosition = move.targetHexagon();
            }
        }
        return bestPosition;
    }

    public Position betterAlgorithmIn2(GameState gameState) {
        Position bestPostion = null, tempPosition, tempOtherColorPosition, tempDirectionPosition, tempOtherColorDirectionPosition, tempSameColorPosition;
        Piece dummyPiece, dummyOtherColorPiece;
        double bestRating = Double.NEGATIVE_INFINITY;
        double tempRating = 0;
        double tempOtherColorRating = 0;
        double tempSameColorRating = 0;
        int counterFreePieces = board * board;
        int counterFreePiecesSameColor = board * board;
        GameState cGameState = gameState.clone();
        Piece.Color otherColor;
        Piece.Color usedColor = cGameState.getSideToMove();

        if (usedColor == Piece.Color.BLUE) {
            otherColor = Piece.Color.RED;
        } else {
            otherColor = Piece.Color.BLUE;
        }

        for (Move move: cGameState.getLegalMoves()) {
            boolean tempHighConnect = false, tempLowConnect = false;
            tempPosition = new Position(move.targetHexagon().getIndex());
            if (cGameState.getPiece(tempPosition) == null)  {
                cGameState.setPiece(tempPosition, new Piece(usedColor));
                cGameState.update(tempPosition);
            }
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

                tempRating = tempRating + calculateRating(tempDirectionPosition, usedColor, cGameState);
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
                    tempOtherColorRating = tempOtherColorRating + calculateRating(tempOtherColorDirectionPosition, otherColor, cGameState);
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
                        counterFreePiecesSameColor--;
                        continue;
                    }
                    tempSameColorPosition = new Position(move2.targetHexagon().getIndex());

                    tempSameColorRating = calculatePieceRating(tempSameColorPosition, usedColor, cGameState);
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
}