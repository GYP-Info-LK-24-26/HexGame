package de.hexgame.nn.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.nn.Model;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TreeNode {
    private static final float EXPLORATION_FACTOR = 1.5f;

    private final List<TreeNode> children = new ArrayList<>();

    private final GameState gameState;
    private Model.Output modelOutput;
    private final float prior;

    private int visits = 0;
    private float valueSum = 0.0f;

    public float getMeanValue() {
        return visits == 0.0f ? 0.0f : valueSum / visits;
    }

    public TreeNode jumpTo(GameState gameState) {
        if (this.gameState.getHalfMoveCounter() >= gameState.getHalfMoveCounter()) {
            return null;
        }

        if (this.gameState.equals(gameState)) {
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

    public float expand(Model model) {
        if (gameState.isFinished()) {
            visits++;
            valueSum += 1.0f;
            return 1.0f;
        }

        if (visits == 0) {
            modelOutput = model.predict(gameState);
            for (Move legalMove : gameState.getLegalMoves()) {
                GameState newGameState = gameState.clone();
                newGameState.makeMove(legalMove);
                float newPrior = modelOutput.policy()[legalMove.targetHexagon().getIndex()];
                children.add(new TreeNode(newGameState, newPrior));
            }
            visits++;
            valueSum += modelOutput.value();
            return modelOutput.value();
        }

        float bestValue = Float.NEGATIVE_INFINITY;
        TreeNode best = null;

        for (TreeNode child : children) {
            float value = (float) (child.getMeanValue() +
                                EXPLORATION_FACTOR * child.prior * Math.sqrt(visits) / (1 + child.visits));
            if (value > bestValue) {
                bestValue = value;
                best = child;
            }
        }

        assert best != null;

        float eval = -best.expand(model);
        visits++;
        valueSum += eval;

        return eval;
    }
}
