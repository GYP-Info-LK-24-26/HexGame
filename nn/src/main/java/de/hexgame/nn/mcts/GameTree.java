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
    private final boolean useNoise;

    public GameTree(GameState gameState, boolean useNoise) {
        root = new TreeNode(null, null, gameState);
        this.useNoise = useNoise;
    }

    public void jumpTo(GameState gameState) {
        TreeNode newRoot = root.jumpTo(gameState);
        if (newRoot == null) {
            newRoot = new TreeNode(null, null, gameState);
        } else if (useNoise && newRoot.getVisits() > 0) {
            newRoot.addDirichletNoise();
        }
        root = newRoot;
    }

    public CompletableFuture<Void> expand(Model model, Executor dispatcher) {
        return root.expand(model, dispatcher, useNoise);
    }

    public Model.Output getCombinedOutput() {
        return root.getCombinedOutput();
    }
}
