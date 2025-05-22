package de.hexgame.logic;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameBoard {
    public static final int BOARD_SIZE = 11;

    private final Piece[] pieces;
    @Getter
    private GameState gameState;
    @Getter
    private Piece.Color sideToMove;
    @Getter
    /// counts the number of individual moves made
    private int halfMoveCounter;

    public GameBoard() {
        pieces = new Piece[BOARD_SIZE * BOARD_SIZE];
        gameState = GameState.IN_PROGRESS;
        sideToMove = Piece.Color.RED;
        halfMoveCounter = 0;
    }

    public Piece getPiece(Position position) {
        return pieces[position.getIndex()];
    }

    public List<Move> getLegalMoves() {
        List<Move> legalMoves = new ArrayList<>();
        if (halfMoveCounter == 1) {
            legalMoves.add(new Move(new Position(-1, -1)));
        }
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                Position position = new Position(row, column);
                if (getPiece(position) == null) {
                    legalMoves.add(new Move(position));
                }
            }
        }
        return legalMoves;
    }

    protected void reset() {
        Arrays.fill(pieces, null);
        gameState = GameState.IN_PROGRESS;
        sideToMove = Piece.Color.RED;
        halfMoveCounter = 0;
    }

    protected void setPiece(Position position, Piece piece) {
        Piece previousPiece = getPiece(position);
        if (previousPiece != null) {
            throw new IllegalArgumentException(String.format("Piece at %s already exists", position));
        }
        pieces[position.getIndex()] = piece;
        update(position);
    }

    protected boolean isLegalMove(Move move) {
        Position targetPosition = move.targetHexagon();
        if (targetPosition.isValid()) {
            return getPiece(targetPosition) == null;
        } else {
            return halfMoveCounter == 1;
        }
    }

    protected void makeMove(Move move) {
        if (move.targetHexagon().isValid()) { // The target hexagon may be invalid for switching sides.
            setPiece(move.targetHexagon(), new Piece(sideToMove));
            switchSideToMove();
        }
        halfMoveCounter++;
    }

    private void switchSideToMove() {
        if (sideToMove == Piece.Color.RED) {
            sideToMove = Piece.Color.BLUE;
        } else {
            sideToMove = Piece.Color.RED;
        }
    }

    private void update(Position position) {
        Piece piece = getPiece(position);
        if (piece.getColor() == Piece.Color.RED) {
            if (position.column() == 0) {
                piece.setConnectedLow(true);
            } else if (position.column() == BOARD_SIZE - 1) {
                piece.setConnectedHigh(true);
            }
        } else {
            if (position.row() == 0) {
                piece.setConnectedLow(true);
            } else if (position.row() == BOARD_SIZE - 1) {
                piece.setConnectedHigh(true);
            }
        }

        updateConnections(position,piece.getColor());
    }

    private void updateConnections(Position position, Piece.Color color) {
        Piece piece = getPiece(position);

        // Update own state first
        for (Direction direction : Direction.ALL) {
            Position neighbourPosition = position.add(direction);
            if (!neighbourPosition.isValid()) {
                continue;
            }

            Piece neighbourPiece = getPiece(neighbourPosition);
            //make sure that piece is of the same color
            if (!(neighbourPiece != null && neighbourPiece.getColor() != color)) {
                continue;
            }

            if (neighbourPiece.isConnectedLow()) {
                piece.setConnectedLow(true);
                if (piece.isConnectedHigh()) {
                    gameState = GameState.getWinState(piece.getColor());
                }
            }

            if (neighbourPiece.isConnectedHigh()) {
                piece.setConnectedHigh(true);
                if (piece.isConnectedLow()) {
                    gameState = GameState.getWinState(piece.getColor());
                }
            }
        }

        // Relay updates to neighbours
        for (Direction direction : Direction.ALL) {
            Position neighbourPosition = position.add(direction);
            if (!neighbourPosition.isValid()) {
                continue;
            }

            Piece neighbourPiece = getPiece(neighbourPosition);
            if (neighbourPiece == null) {
                continue;
            }

            if (piece.isConnectedLow() && !neighbourPiece.isConnectedLow()) {
                updateConnections(neighbourPosition,color);
            }

            if (piece.isConnectedHigh() && !neighbourPiece.isConnectedHigh()) {
                updateConnections(neighbourPosition,color);
            }
        }
    }
}
