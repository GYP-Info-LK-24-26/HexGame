package de.hexgame.nn.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.nn.Model;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameTree {
    private TreeNode root;
    @Getter
    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor();

    public GameTree(GameState gameState) {
        root = new TreeNode(null, null, gameState);
    }

    public void jumpTo(GameState gameState) {
        root = root.jumpTo(gameState);
        if (root != null) {
            root.addDirichletNoise();
        } else {
            root = new TreeNode(null, null, gameState);
        }
    }

    public CompletableFuture<Void> expand(Model model, Executor dispatcher, boolean addNoise) {
        return root.expand(model, dispatcher, addNoise);
    }

    public Model.Output getCombinedOutput() {
        return root.getCombinedOutput();
    }
}
