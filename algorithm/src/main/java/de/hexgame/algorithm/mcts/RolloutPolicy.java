package de.hexgame.algorithm.mcts;

import de.hexgame.logic.Direction;
import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Position;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static de.hexgame.logic.GameState.*;

/**
 * Rollout (playout) move-selection policy for MCTS simulations.
 * Combines save-bridge, Last-Good-Reply (LGR-1), and a pattern-based
 * weighted random fallback using only local features (no BFS).
 */
class RolloutPolicy {
    private static final int TOTAL_CELLS = BOARD_SIZE * BOARD_SIZE;

    static final int[] LGR = new int[2 * TOTAL_CELLS];

    static {
        Arrays.fill(LGR, -1);
    }

    static int selectMove(GameState state) {
        return selectMove(state, null);
    }

    static int selectMove(GameState state, boolean[] pruned) {
        int lastMove = state.getLastMove();
        if (lastMove != -1) {
            // Save-bridge: if the opponent's last move attacks one of our bridges,
            // deterministically play the other carrier cell to maintain the connection.
            int saveBridge = findSaveBridge(state, lastMove);
            if (saveBridge != -1) {
                return saveBridge;
            }

            int lgr = LGR[(state.getSideToMove() - RED) * TOTAL_CELLS + lastMove];
            if (lgr != -1 && state.getPiece(lgr) == NO_PIECE) {
                return lgr;
            }
        }

        return randomMove(state, pruned);
    }

    /**
     * Scans the 6 neighbours of {@code attackCell} in circular order looking for
     * the pattern [friendly] [empty] [friendly]. The empty cell is the save move
     * that maintains the bridge connection. Uses MoHex's state-machine approach
     * with a random start direction to avoid bias when multiple bridges are attacked.
     *
     * @return the index of the save move, or -1 if no bridge is threatened
     */
    private static int findSaveBridge(GameState state, int attackCell) {
        int myColor = state.getSideToMove();
        int r = attackCell / BOARD_SIZE;
        int c = attackCell % BOARD_SIZE;
        Direction[] dirs = Direction.ALL;
        int start = ThreadLocalRandom.current().nextInt(6);

        // State machine: 0=looking, 1=saw friendly, 2=saw friendly+empty
        int s = 0;
        int saveIdx = -1;
        // Loop 8 times (not 6) to handle wrap-around of the C-E-C pattern
        for (int j = 0; j < 8; j++) {
            Direction d = dirs[(j + start) % 6];
            int nr = r + d.getDeltaRow();
            int nc = c + d.getDeltaColumn();
            if (nr < 0 || nr >= BOARD_SIZE || nc < 0 || nc >= BOARD_SIZE) {
                s = 0;
                continue;
            }
            int ni = nr * BOARD_SIZE + nc;
            int p = state.getPiece(ni);
            boolean mine = p != NO_PIECE && p == myColor;
            boolean empty = p == NO_PIECE;

            if (s == 0) {
                if (mine) s = 1;
            } else if (s == 1) {
                if (mine) { /* stay in state 1 */ }
                else if (empty) { s = 2; saveIdx = ni; }
                else s = 0; // opponent stone breaks the pattern
            } else { // s == 2
                if (mine) return saveIdx; // matched [friendly] [empty] [friendly]
                else s = 0;
            }
        }
        return -1;
    }

    static int randomMove(GameState state) {
        return randomMove(state, null);
    }

    private static int randomMove(GameState state, boolean[] pruned) {
        int r = ThreadLocalRandom.current().nextInt(TOTAL_CELLS);
        int fallback = -1;
        for (int i = 0; i < TOTAL_CELLS; i++) {
            int idx = (r + i * 7) % TOTAL_CELLS;
            if (state.getPiece(idx) == NO_PIECE) {
                if (pruned == null || !pruned[idx]) {
                    return idx;
                }
                if (fallback == -1) {
                    fallback = idx;
                }
            }
        }
        // If all empty cells are pruned, fall back to any empty cell
        if (fallback != -1) return fallback;
        throw new IllegalStateException("No empty cell found");
    }
}
