package de.hexgame.nn.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.nn.Model;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static de.hexgame.logic.GameState.BOARD_SIZE;

public class TreeNode {
    private static final float EXPLORATION_FACTOR = 1.5f;
    private static final float VIRTUAL_LOSS = 1.0f;

    private TreeNode parent;
    private final List<TreeNode> children = new ArrayList<>();

    private final Move move;
    @Getter
    private GameState gameState;
    private Model.Output modelOutput;

    @Getter
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

    public TreeNode jumpTo(GameState gameState) {
        if (this.gameState.getHalfMoveCounter() > gameState.getHalfMoveCounter()) {
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

    /**
     * Walk the tree to select a leaf node for NN evaluation. Applies virtual loss along the path.
     *
     * @return the leaf node needing evaluation, or null if a terminal node was reached (already backpropagated)
     */
    public TreeNode select() {
        visits++;
        valueSum += VIRTUAL_LOSS;

        if (visits == 1 && move != null) {
            gameState = gameState.clone();
            gameState.makeMove(move);
        }

        if (gameState.isFinished()) {
            backpropagate(-1.1f);
            return null;
        }

        if (visits == 1) {
            for (Move legalMove : gameState.getLegalMoves()) {
                children.add(new TreeNode(this, legalMove, gameState));
            }
            return this;
        }

        return getBestChild().select();
    }

    public void applyOutput(Model.Output output) {
        modelOutput = output;
        backpropagate(modelOutput.value());
    }

    private void backpropagate(float eval) {
        valueSum += eval - VIRTUAL_LOSS;
        if (parent != null) {
            parent.backpropagate(-eval);
        }
    }

    private float getPrior(TreeNode child) {
        if (modelOutput == null) {
            return 1.0f / (BOARD_SIZE * BOARD_SIZE) + ThreadLocalRandom.current().nextFloat(1e-4f);
        }
        return modelOutput.policy()[child.move.getIndex()];
    }

    private TreeNode getBestChild() {
        float bestValue = Float.NEGATIVE_INFINITY;
        TreeNode best = null;

        float sumVisitedPriors = 0.0f;
        for (TreeNode child : children) {
            if (child.visits > 0) {
                sumVisitedPriors += getPrior(child);
            }
        }
        float fpuQ = getMeanValue() - 0.2f * (float) Math.sqrt(sumVisitedPriors);

        for (TreeNode child : children) {
            float Q = child.visits == 0 ? fpuQ : -child.getMeanValue();
            float U = (float) (EXPLORATION_FACTOR * getPrior(child) * Math.sqrt(visits) / (1 + child.visits));
            float value = Q + U;
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

    public void print(String prefix, String childrenPrefix) {
        System.out.println(prefix + move);
        for (Iterator<TreeNode> it = children.iterator(); it.hasNext(); ) {
            TreeNode next = it.next();
            if (next.getVisits() == 0) continue;
            if (it.hasNext()) {
                next.print(childrenPrefix + "|-- ", childrenPrefix + "|   ");
            } else {
                next.print(childrenPrefix + "--- ", childrenPrefix + "    ");
            }
        }
    }
}
