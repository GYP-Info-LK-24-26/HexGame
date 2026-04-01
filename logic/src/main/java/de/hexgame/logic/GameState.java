package de.hexgame.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * This is the class that runs the Game, used by {@link Game}
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameState implements Serializable, Cloneable {
    public static final int BOARD_SIZE = 11;
    public static final int TOTAL_CELLS = BOARD_SIZE * BOARD_SIZE;
    public static final int NO_PIECE = 0;
    public static final int RED = 1;
    public static final int BLUE = 2;
    public static final int COLOR_SUM = 3;
    public static final int EDGE_RED_LOW = TOTAL_CELLS;
    public static final int EDGE_RED_HIGH = TOTAL_CELLS + 1;
    public static final int EDGE_BLUE_LOW = TOTAL_CELLS + 2;
    public static final int EDGE_BLUE_HIGH = TOTAL_CELLS + 3;
    public static final int GROUP_COUNT = TOTAL_CELLS + 4;

    // Precomputed neighbor table: for each cell index, the array of valid neighbor indices
    public static final int[][] NEIGHBORS = new int[TOTAL_CELLS][];

    static {
        for (int idx = 0; idx < TOTAL_CELLS; idx++) {
            int row = idx / BOARD_SIZE;
            int col = idx % BOARD_SIZE;
            int count = 0;
            int[] tmp = new int[6];
            for (Direction d : Direction.ALL) {
                int nr = row + d.getDeltaRow();
                int nc = col + d.getDeltaColumn();
                if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE) {
                    tmp[count++] = nr * BOARD_SIZE + nc;
                }
            }
            NEIGHBORS[idx] = new int[count];
            System.arraycopy(tmp, 0, NEIGHBORS[idx], 0, count);
        }
    }

    private int[] pieces;
    @Getter
    private boolean finished;
    @Getter
    private int sideToMove;
    @Getter
    /// counts the number of individual moves made
    private int halfMoveCounter;
    @Getter
    private int lastMove;
    @Getter
    private UnionFind groups;

    public GameState() {
        pieces = new int[TOTAL_CELLS];
        finished = false;
        sideToMove = RED;
        halfMoveCounter = 0;
        lastMove = -1;
        groups = new UnionFind(GROUP_COUNT);
    }

    public int getPiece(Position position) {
        return pieces[position.getIndex()];
    }

    public int getPiece(int index) {
        return pieces[index];
    }

    /**
     * is O(n²) complexity
     * @return all moves that can be made
     */
    public List<Move> getLegalMoves() {
        List<Move> legalMoves = new ArrayList<>();
        if (halfMoveCounter == 1) {
            legalMoves.add(new Move(new Position(lastMove)));
        }
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                Position position = new Position(row, column);
                if (getPiece(position) == NO_PIECE) {
                    legalMoves.add(new Move(position));
                }
            }
        }
        return legalMoves;
    }

    public void reset() {
        Arrays.fill(pieces, NO_PIECE);
        finished = false;
        sideToMove = RED;
        halfMoveCounter = 0;
    }

    public void setPiece(Position position, int piece) {
        setPiece(position.getIndex(), piece);
    }

    public void setPiece(int index, int piece) {
        int previousPiece = getPiece(index);
        if (previousPiece != NO_PIECE) {
            throw new IllegalArgumentException(String.format("Piece at %s already exists", new Position(index)));
        }
        pieces[index] = piece;
        update(index, piece);
    }

    public boolean isLegalMove(Move move) {
        Position targetPosition = move.targetHexagon();
        return getPiece(targetPosition) == NO_PIECE || (halfMoveCounter == 1 && targetPosition.getIndex() == lastMove);
    }

    //this makes a move,it also accommodates the change of color by a player by not switching the color that is currently at play
    public void makeMove(Move move) {
        if (!isLegalMove(move)) {
            throw new IllegalStateException(
                    String.format("%s tried to play the illegal move %s", sideToMove, move)
            );
        }

        if (getPiece(move.targetHexagon()) == NO_PIECE) { // The target hexagon may be occupied for switching sides.
            setPiece(move.targetHexagon(), sideToMove);
            switchSideToMove();
        }
        lastMove = move.getIndex();

        halfMoveCounter++;
    }

    public void makeMoveFast(int index) {
        if (pieces[index] == NO_PIECE) {
            pieces[index] = sideToMove;
            update(index, sideToMove);
            sideToMove = COLOR_SUM - sideToMove;
        }

        lastMove = index;
        halfMoveCounter++;
    }

    public void switchSideToMove() {
        sideToMove = COLOR_SUM - sideToMove;
    }

    //updates the connected pieces so that the connection states are up to play
    private void update(int index, int piece) {
        for (int n : NEIGHBORS[index]) {
            if (pieces[n] == piece) {
                groups.union(n, index);
            }
        }

        unionWithGoalEdge(groups, index, piece);

        if (piece == RED) {
            if (groups.find(EDGE_RED_LOW) == groups.find(EDGE_RED_HIGH)) {
                finished = true;
            }
        } else {
            if (groups.find(EDGE_BLUE_LOW) == groups.find(EDGE_BLUE_HIGH)) {
                finished = true;
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
            clone.groups = groups.clone();
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
        for (int p : pieces) {
            h = 31 * h + p;
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

    /**
     * Unions the cell at {@code index} with its goal-side edge virtual node
     * ({@link #GROUP_LOW} or {@link #GROUP_HIGH}) if it lies on that edge.
     * RED connects column 0 (low) to column BOARD_SIZE-1 (high).
     * BLUE connects row 0 (low) to row BOARD_SIZE-1 (high).
     */
    public static void unionWithGoalEdge(UnionFind groups, int index, int piece) {
        if (piece == RED) {
            int column = index % BOARD_SIZE;
            if (column == 0) {
                groups.union(index, EDGE_RED_LOW);
            } else if (column == BOARD_SIZE - 1) {
                groups.union(index, EDGE_RED_HIGH);
            }
        } else {
            int row = index / BOARD_SIZE;
            if (row == 0) {
                groups.union(index, EDGE_BLUE_LOW);
            } else if (row == BOARD_SIZE - 1) {
                groups.union(index, EDGE_BLUE_HIGH);
            }
        }
    }
}
