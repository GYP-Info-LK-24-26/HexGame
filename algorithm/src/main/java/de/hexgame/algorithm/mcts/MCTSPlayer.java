package de.hexgame.algorithm.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;

public class MCTSPlayer implements Player {
    public static int iceRuns = 0;
    public static int movesPruned = 0;

    private static final long[] TIME_BUDGETS = {200, 500, 1000, 2000, 4000, 8000};

    private final long timeBudgetMs;
    private GameTree tree;

    public MCTSPlayer() {
        this(3); // default: 2000ms
    }

    public MCTSPlayer(int difficulty) {
        this.timeBudgetMs = TIME_BUDGETS[Math.max(0, Math.min(difficulty, TIME_BUDGETS.length - 1))];
    }

    @Override
    public String getName() {
        return "MCTS Player";
    }

    @Override
    public Move think(GameState gameState) {
        iceRuns = movesPruned = 0;
        if (tree == null) {
            tree = new GameTree(gameState);
        } else {
            tree.jumpTo(gameState);
        }

        tree.runSimulations(timeBudgetMs);
        System.out.printf("%d ICE runs pruned %d moves%n", iceRuns, movesPruned);
        return tree.getBestMove();
    }
}
