package de.hexgame.nn.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.nn.Model;
import org.nd4j.linalg.api.ndarray.INDArray;

import static de.hexgame.logic.GameState.BOARD_SIZE;

public class GameTree {
    private TreeNode root;

    public GameTree(GameState gameState) {
        root = new TreeNode(gameState, 0.0f);
    }

    public void jumpTo(GameState gameState) {
        root = root.jumpTo(gameState);
        if (root == null) {
            root = new TreeNode(gameState, 0.0f);
        }
    }

    public void expand(Model model) {
        root.expand(model);
    }

    public Model.Output getCombinedOutput() {
        float[] policy = new float[BOARD_SIZE * BOARD_SIZE];
        return new Model.Output(policy, root.getMeanValue());
    }
}
