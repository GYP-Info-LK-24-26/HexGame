package de.hexgame.nn.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.nn.Model;

public class GameTree {
    private TreeNode root;

    public GameTree(GameState gameState) {
        root = new TreeNode(null, null, gameState);
    }

    public void jumpTo(GameState gameState) {
        TreeNode newRoot = root.jumpTo(gameState);
        if (newRoot == null) {
            newRoot = new TreeNode(null, null, gameState.clone());
        }
        root = newRoot;
    }

    public TreeNode selectLeaf() {
        return root.select();
    }

    public Model.Output getCombinedOutput() {
        return root.getCombinedOutput();
    }

    public void print() {
        root.print("", "");
    }
}
