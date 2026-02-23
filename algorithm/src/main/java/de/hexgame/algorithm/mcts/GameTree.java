package de.hexgame.algorithm.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;

public class GameTree {
    private TreeNode root;

    public GameTree(GameState gameState) {
        root = new TreeNode(null, null, gameState.clone());
    }

    public void jumpTo(GameState gameState) {
        TreeNode newRoot = root.jumpTo(gameState);
        if (newRoot == null) {
            newRoot = new TreeNode(null, null, gameState.clone());
        }
        root = newRoot;
    }

    public void runSimulations(long timeBudgetMs) {
        long deadline = System.currentTimeMillis() + timeBudgetMs;
        int count = 0;
        while (System.currentTimeMillis() < deadline) {
            root.simulate();
            count++;
        }
        System.out.printf("MCTS: %d simulations in %dms%n", count, timeBudgetMs);
    }

    public String exportDot(int maxDepth, int minVisits) {
        return root.toDot(maxDepth, minVisits);
    }

    public String exportDot() {
        return exportDot(1, 1);
    }

    public Move getBestMove() {
        Move move = root.getBestMove();
        System.out.println("Win chance: " + move.winChance());
        //System.out.println(exportDot());
        return move;
    }
}
