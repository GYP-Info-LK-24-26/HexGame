package de.hexgame.nn.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.nn.Model;
import org.apache.commons.math3.distribution.GammaDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static de.hexgame.logic.GameState.BOARD_SIZE;

public class TreeNode {
    private static final float EXPLORATION_FACTOR = 1.5f;
    private static final float VIRTUAL_LOSS = 1.0f;

    private TreeNode parent;
    private final List<TreeNode> children = new ArrayList<>();

    private final Move move;
    private GameState gameState; // lazily evaluated
    private Model.Output modelOutput;

    private int visits = 0;
    private float valueSum = 0.0f;

    public TreeNode(TreeNode parent, Move move, GameState gameState) {
        this.parent = parent;
        this.move = move;
        this.gameState = gameState;
    }

    public float getMeanValue() {
        return visits == 0 ? 0.0f : valueSum / visits;
    }

    public void addDirichletNoise() {
        GammaDistribution gamma = new GammaDistribution(0.1, 1.0);
        float[] samples = new float[BOARD_SIZE * BOARD_SIZE];
        float sum = 0.0f;
        for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
            float sample = (float) gamma.sample();
            samples[i] = sample;
            sum += sample;
        }

        final float epsilon = 0.25f;

        for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
            modelOutput.policy()[i] = modelOutput.policy()[i] * (1 - epsilon) + samples[i] * epsilon / sum;
        }
    }

    public TreeNode jumpTo(GameState gameState) {
        if (this.gameState.getHalfMoveCounter() >= gameState.getHalfMoveCounter()) {
            return null;
        }

        if (this.gameState.equals(gameState)) {
            parent = null;
            return this;
        }

        for (TreeNode child : children) {
            TreeNode target = child.jumpTo(gameState);
            if (target != null) {
                return target;
            }
        }

        return null;
    }

    public CompletableFuture<Void> expand(Model model, Executor dispatcher, boolean addNoise) {
        visits++;
        valueSum -= VIRTUAL_LOSS;
        if (visits == 1 && move != null) {
            gameState = gameState.clone();
            gameState.makeMove(move);
        }

        if (gameState.isFinished()) {
            backpropagate(-1.0f);
            return CompletableFuture.completedFuture(null);
        }

        if (visits == 1) {
            for (Move legalMove : gameState.getLegalMoves()) {
                children.add(new TreeNode(this, legalMove, gameState));
            }

            return model.predict(gameState).thenAcceptAsync(output -> {
                modelOutput = output;
                if (addNoise) {
                    addDirichletNoise();
                }
                backpropagate(modelOutput.value());
            }, dispatcher);
        }

        TreeNode best = getBestChild();

        return best.expand(model, dispatcher, false);
    }

    private void backpropagate(float eval) {
        valueSum += eval + VIRTUAL_LOSS;
        if (parent != null) {
            parent.backpropagate(-eval);
        }
    }

    private TreeNode getBestChild() {
        float bestValue = Float.NEGATIVE_INFINITY;
        TreeNode best = null;

        for (TreeNode child : children) {
            final float prior = modelOutput == null ? 1.0f / (BOARD_SIZE * BOARD_SIZE) : modelOutput.policy()[child.move.getIndex()];
            float value = (float) (-child.getMeanValue() +
                                EXPLORATION_FACTOR * prior * Math.sqrt(visits) / (1 + child.visits));
            if (value > bestValue) {
                bestValue = value;
                best = child;
            }
        }

        assert best != null;
        return best;
    }

    public Model.Output getCombinedOutput() {
        float[] policy = new float[BOARD_SIZE * BOARD_SIZE];
        for (TreeNode child : children) {
            policy[child.move.getIndex()] = child.visits;
        }
        return new Model.Output(policy, getMeanValue());
    }
}
