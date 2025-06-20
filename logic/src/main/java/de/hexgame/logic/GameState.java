package de.hexgame.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This is the class that runs the Game, used by {@link Game}
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameState implements Cloneable {
    public static final int BOARD_SIZE = 11;

    private Piece[] pieces;
    @Getter
    private boolean finished;
    @Getter
    private Piece.Color sideToMove;
    @Getter
    /// counts the number of individual moves made
    private int halfMoveCounter;
    private Position lastChangedPosition;
    //keeps track of every listener hook
    private List<PlayerMoveListener> playerMoveListeners;

    /**
     * this adds a listener for player moves
     * @param playerMoveListener the listener
     */
    public void addPlayerMoveListener(PlayerMoveListener playerMoveListener) {
        if(playerMoveListeners != null)playerMoveListeners.add(playerMoveListener);
    }

    public GameState() {
        pieces = new Piece[BOARD_SIZE * BOARD_SIZE];
        finished = false;
        sideToMove = Piece.Color.RED;
        halfMoveCounter = 0;
        playerMoveListeners = new ArrayList<>();
    }

    public Piece getPiece(Position position) {
        return pieces[position.getIndex()];
    }

    /**
     * is O(n²) complexity
     * @return all moves that can be made
     */
    public List<Move> getLegalMoves() {
        List<Move> legalMoves = new ArrayList<>();
        if (halfMoveCounter == 1) {
            legalMoves.add(new Move(lastChangedPosition));
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

    public void reset() {
        Arrays.fill(pieces, null);
        finished = false;
        sideToMove = Piece.Color.RED;
        halfMoveCounter = 0;
    }

    public void resetOnePiece(int position) {
        pieces[position] = null;
    }

    public void setPiece(Position position, Piece piece) {
        Piece previousPiece = getPiece(position);
        if (previousPiece != null) {
            throw new IllegalArgumentException(String.format("Piece at %s already exists", position));
        }
        pieces[position.getIndex()] = piece;
        update(position);
    }

    public boolean isLegalMove(Move move) {
        Position targetPosition = move.targetHexagon();
        return getPiece(targetPosition) == null || targetPosition.equals(lastChangedPosition);
    }

    //this makes a move,it also accommodates the change of color by a player by not switching the color that is currently at play
    public void makeMove(Move move) {
        if (isLegalMove(move)) { // The target hexagon may be invalid for switching sides.
            setPiece(move.targetHexagon(), new Piece(sideToMove));
            switchSideToMove();
        }else if(!move.targetHexagon().equals(lastChangedPosition))
            throw new IllegalStateException(
                String.format("%s tried to play the illegal move %s", sideToMove, move)
        );
        lastChangedPosition = move.targetHexagon();
        playerMoveListeners.forEach(listener -> listener.onPlayerMove(move.targetHexagon()));
        halfMoveCounter++;
    }

    private void switchSideToMove() {
        if (sideToMove == Piece.Color.RED) {
            sideToMove = Piece.Color.BLUE;
        } else {
            sideToMove = Piece.Color.RED;
        }
    }

    //updates the connected pieces so that the connection states are up to play
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

        updateConnections(position);
    }

    /**
     * recursive function to update the connecting states of all neighbouring pieces
     * @param position the position to be updated
     */
    private void updateConnections(Position position) {
        Piece piece = getPiece(position);

        // Update own state first
        for (Direction direction : Direction.ALL) {
            Position neighbourPosition = position.add(direction);
            if (!neighbourPosition.isValid()) {
                continue;
            }

            Piece neighbourPiece = getPiece(neighbourPosition);
            //make sure that piece is of the same color
            if (neighbourPiece == null || neighbourPiece.getColor() != piece.getColor()) {
                continue;
            }

            if (neighbourPiece.isConnectedLow()) {
                piece.setConnectedLow(true);
                if (piece.isConnectedHigh()) {
                    finished = true;
                }
            }

            if (neighbourPiece.isConnectedHigh()) {
                piece.setConnectedHigh(true);
                if (piece.isConnectedLow()) {
                    finished = true;
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
            if (neighbourPiece == null || neighbourPiece.getColor() != piece.getColor()) {
                continue;
            }

            if (piece.isConnectedLow() && !neighbourPiece.isConnectedLow()) {
                updateConnections(neighbourPosition);
            }

            if (piece.isConnectedHigh() && !neighbourPiece.isConnectedHigh()) {
                updateConnections(neighbourPosition);
            }
        }
    }

    @Override
    public GameState clone() {
        try {
            GameState clone = (GameState) super.clone();
            clone.pieces = pieces.clone();
            for (int i = 0; i < pieces.length; i++) {
                clone.pieces[i] = pieces[i].clone();
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
