package de.hexgame.algorithm.mcts;

import de.hexgame.logic.GameState;

import static de.hexgame.logic.GameState.TOTAL_CELLS;

/**
 * Result of the Inferior Cell Engine computation.
 * Contains dead cells (prunable), captured cells (fill-in), and the resulting game state.
 */
public class ICEResult {
    /** Cells dead for both colors — can be pruned from MCTS expansion. */
    public final boolean[] dead = new boolean[TOTAL_CELLS];
    /** Cells captured by RED — should be filled with RED stones. */
    public final boolean[] capturedByRed = new boolean[TOTAL_CELLS];
    /** Cells captured by BLUE — should be filled with BLUE stones. */
    public final boolean[] capturedByBlue = new boolean[TOTAL_CELLS];
    /** Game state after filling all captured cells. */
    public GameState filledState;
}
