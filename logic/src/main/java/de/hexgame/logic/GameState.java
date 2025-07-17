package de.hexgame.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


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

    public GameState() {
        pieces = new Piece[BOARD_SIZE * BOARD_SIZE];
        finished = false;
        sideToMove = Piece.Color.RED;
        halfMoveCounter = 0;
    }

    public Piece getPiece(Position position) {
        return pieces[position.getIndex()];
    }

    public Piece getPiece(int index) {
        return pieces[index];
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
        return getPiece(targetPosition) == null || (halfMoveCounter == 1 && targetPosition.equals(lastChangedPosition));
    }

    //this makes a move,it also accommodates the change of color by a player by not switching the color that is currently at play
    public void makeMove(Move move) {
        if (!isLegalMove(move)) {
            throw new IllegalStateException(
                    String.format("%s tried to play the illegal move %s", sideToMove, move)
            );
        }

        if (getPiece(move.targetHexagon()) == null) { // The target hexagon may be occupied for switching sides.
            setPiece(move.targetHexagon(), new Piece(sideToMove));
            switchSideToMove();
        }
        lastChangedPosition = move.targetHexagon();

        halfMoveCounter++;
    }

    public void switchSideToMove() {
        if (sideToMove == Piece.Color.RED) {
            sideToMove = Piece.Color.BLUE;
        } else {
            sideToMove = Piece.Color.RED;
        }
    }

    //updates the connected pieces so that the connection states are up to play
    public void update(Position position) {
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

    /**
     * Clones the Game state and all pieces so that the cloned pieces do not interfere with the original ones
     * @return the cloned Game state with cloned pieces
     * @see #cloneWithoutListeners()
     */
    @Override
    public GameState clone() {
        try {
            GameState clone = (GameState) super.clone();
            clone.pieces = pieces.clone();
            for (int i = 0; i < pieces.length; i++) {
                if (pieces[i] != null) {
                    clone.pieces[i] = pieces[i].clone();
                }
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * Clones the Game state and all pieces so that the cloned pieces do not interfere with the original ones<br>
     * the listeners are cleared here so that one may use this for non-interactive thinking
     * @return the cloned Game state with cloned pieces and without listeners
     * @see #clone()
     */
    @Deprecated(forRemoval = true)
    public GameState cloneWithoutListeners(){
        GameState clone = clone();
        return clone;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(pieces), halfMoveCounter);
    }

    public long hashCodeLong() {
        long h = 1;
        for (Piece p : pieces) {
            long code;
            if (p == null) {
                code = 0;
            } else {
                code = (p.getColor() == Piece.Color.RED) ? 1 : 2;
            }
            h = 31 * h + code;
        }
        h = 31 * h + (halfMoveCounter == 1 ? 1 : 0);
        return Long.reverse(h);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GameState gameState = (GameState) o;
        return halfMoveCounter == gameState.halfMoveCounter && Objects.deepEquals(pieces, gameState.pieces);
    }
}
