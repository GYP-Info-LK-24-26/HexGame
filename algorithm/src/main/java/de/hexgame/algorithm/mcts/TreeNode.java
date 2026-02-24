package de.hexgame.algorithm.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static de.hexgame.logic.GameState.*;

public class TreeNode {
    private static final float EXPLORATION_FACTOR = 0.7f;//1.4f;
    private static final float RAVE_B_SQUARED = 0.1f * 0.1f;
    private static final int TOTAL_CELLS = BOARD_SIZE * BOARD_SIZE;
    private static final boolean[] EMPTY_MOVES = new boolean[TOTAL_CELLS];

    private TreeNode parent;
    private final List<TreeNode> children = new ArrayList<>();

    private final Move move;
    private GameState gameState; // lazily materialized

    @Getter
    private int visits = 0;
    private float valueSum = 0.0f;

    private int raveVisits = 0;
    private float raveValueSum = 0.0f;

    private record RolloutResult(float value, boolean[] redMoves, boolean[] blueMoves) {}

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

    public void simulate() {
        // Selection: walk down tree using UCT
        TreeNode node = this;
        while (!node.children.isEmpty()) {
            if (node.visits == 50) {
                MCTSPlayer.iceRuns++;
                boolean[] deadCells = InferiorCellEngine.computeDeadCells(node.gameState);
                TreeNode firstChild = node.children.getFirst();
                node.children.removeIf(c -> {
                    if (deadCells[c.move.getIndex()]) {
                        MCTSPlayer.movesPruned++;
                        return true;
                    }
                    return false;
                });
                // Safety: never prune all children — restore if everything was removed
                if (node.children.isEmpty()) {
                    MCTSPlayer.movesPruned--;
                    node.children.add(firstChild);
                }
            }
            node = node.selectChild();
        }

        // Lazy state materialization
        if (node.visits == 0 && node.move != null) {
            node.gameState = node.gameState.clone();
            node.gameState.makeMove(node.move);
        }

        // Terminal node
        if (node.gameState.isFinished()) {
            // The player who just moved won — that's the parent's side, so -1 from node's perspective
            node.backpropagate(-1.0f, EMPTY_MOVES, EMPTY_MOVES);
            return;
        }

        // Expansion: if node has been visited before, expand it
        if (node.visits > 0) {
            for (Move legalMove : node.gameState.getLegalMoves()) {
                node.children.add(new TreeNode(node, legalMove, node.gameState));
            }
            node = node.selectChild();
            // Materialize state for the new child
            node.gameState = node.gameState.clone();
            node.gameState.makeMove(node.move);

            if (node.gameState.isFinished()) {
                node.backpropagate(-1.0f, EMPTY_MOVES, EMPTY_MOVES);
                return;
            }
        }

        // Rollout
        RolloutResult result = rollout(node.gameState);
        node.backpropagate(result.value(), result.redMoves(), result.blueMoves());
    }

    private TreeNode selectChild() {
        float bestValue = Float.NEGATIVE_INFINITY;
        TreeNode best = null;
        double logParentVisits = Math.log(visits);

        for (TreeNode child : children) {
            float value;
            if (child.visits == 0) {
                // Unvisited: always prioritize over visited nodes (FPU).
                // Rank among themselves by RAVE value if available, random otherwise.
                if (child.raveVisits > 0) {
                    value = 200f - child.raveValueSum / child.raveVisits;
                } else {
                    value = 100f;
                }
            } else {
                float mcValue = -child.getMeanValue();
                float raveValue = child.raveVisits > 0 ? -child.raveValueSum / child.raveVisits : mcValue;

                //float beta = (float) Math.sqrt(RAVE_K / (3.0 * child.visits + RAVE_K));
                float beta = (float) child.raveVisits / (child.visits + child.raveVisits + 4 * RAVE_B_SQUARED * child.visits * child.raveVisits);
                float exploration = (float) (EXPLORATION_FACTOR * Math.sqrt(logParentVisits / child.visits));

                value = (1 - beta) * mcValue + beta * raveValue + exploration;
            }
            if (value > bestValue) {
                bestValue = value;
                best = child;
            }
        }

        assert best != null;
        return best;
    }

    private RolloutResult rollout(GameState state) {
        boolean[] redMoves = new boolean[TOTAL_CELLS];
        boolean[] blueMoves = new boolean[TOTAL_CELLS];
        int[] moveHistory = new int[TOTAL_CELLS];
        int moveCount = 0;

        GameState sim = state.clone();
        while (!sim.isFinished()) {
            int side = sim.getSideToMove();
            int move = RolloutPolicy.selectMove(sim);
            moveHistory[moveCount++] = move;
            if (side == RED) {
                redMoves[move] = true;
            } else {
                blueMoves[move] = true;
            }
            sim.makeMoveFast(move);
        }

        // sim.getSideToMove() is the LOSING side (switched after winning move).
        int loser = sim.getSideToMove();
        float value = state.getSideToMove() == loser ? -1.0f : 1.0f;

        // Update LGR-1: for each winner's move, record it as the good reply to the opponent's preceding move
        int winner = COLOR_SUM - loser;
        int winnerOffset = (winner - RED) * TOTAL_CELLS;
        int currentSide = state.getSideToMove();
        int prevOpponentMove = state.getLastMove();

        for (int i = 0; i < moveCount; i++) {
            if (currentSide == winner && prevOpponentMove != -1) {
                RolloutPolicy.LGR[winnerOffset + prevOpponentMove] = moveHistory[i];
            }
            if (currentSide != winner) {
                prevOpponentMove = moveHistory[i];
            }
            currentSide = COLOR_SUM - currentSide;
        }

        return new RolloutResult(value, redMoves, blueMoves);
    }

    private void backpropagate(float eval, boolean[] redMoves, boolean[] blueMoves) {
        visits++;
        valueSum += eval;

        // Update RAVE stats for children whose moves appeared in the rollout
        if (!children.isEmpty()) {
            // The side to move at this node is the side that picks among children
            int sideToMove = gameState.getSideToMove();
            boolean[] relevantMoves = sideToMove == RED ? redMoves : blueMoves;

            for (TreeNode child : children) {
                int idx = child.move.targetHexagon().row() * BOARD_SIZE + child.move.targetHexagon().column();
                if (relevantMoves[idx]) {
                    child.raveVisits++;
                    // eval is from this node's perspective; child values are negated, so pass -eval
                    // to keep consistent with how we negate in selectChild
                    child.raveValueSum -= eval;
                }
            }
        }

        if (parent != null) {
            parent.backpropagate(-eval, redMoves, blueMoves);
        }
    }

    public String toDot(int maxDepth, int minVisits) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph MCTS {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [shape=record, style=filled];\n");
        writeDot(sb, new AtomicInteger(0), 0, maxDepth, minVisits);
        sb.append("}\n");
        return sb.toString();
    }

    private int writeDot(StringBuilder sb, AtomicInteger counter, int depth, int maxDepth, int minVisits) {
        int id = counter.getAndIncrement();
        float winRate = (getMeanValue() + 1.0f) / 2.0f;
        String color = winRateColor(winRate);

        if (move == null) {
            sb.append(String.format("  n%d [label=\"Root|visits: %d\", fillcolor=\"%s\"];\n", id, visits, color));
        } else {
            float raveWinRate = raveVisits > 0 ? (raveValueSum / raveVisits + 1.0f) / 2.0f : 0;
            sb.append(String.format("  n%d [label=\"(%d,%d)|v:%d w:%.0f%%|rv:%d rw:%.0f%%\", fillcolor=\"%s\"];\n",
                    id, move.targetHexagon().row(), move.targetHexagon().column(),
                    visits, winRate * 100, raveVisits, raveWinRate * 100, color));
        }

        if (depth < maxDepth) {
            for (TreeNode child : children) {
                if (child.visits >= minVisits) {
                    int childId = child.writeDot(sb, counter, depth + 1, maxDepth, minVisits);
                    sb.append(String.format("  n%d -> n%d [label=\"(%d,%d)\"];\n",
                            id, childId, child.move.targetHexagon().row(), child.move.targetHexagon().column()));
                }
            }
        }

        return id;
    }

    private static String winRateColor(float winRate) {
        // Gradient: red (0%) -> yellow (50%) -> green (100%)
        int r, g;
        if (winRate < 0.5f) {
            r = 255;
            g = (int) (winRate * 2 * 255);
        } else {
            r = (int) ((1.0f - winRate) * 2 * 255);
            g = 255;
        }
        return String.format("#%02x%02x00", r, g);
    }

    public Move getBestMove() {
        int bestVisits = -1;
        Move best = null;
        float bestWinRate = 0;

        for (TreeNode child : children) {
            if (child.visits > bestVisits) {
                bestVisits = child.visits;
                best = child.move;
                // Win rate from parent's perspective: negate child's mean value
                bestWinRate = -child.getMeanValue();
            }
        }

        if (best == null) return null;
        // Convert from [-1, 1] to [0, 1] win chance
        double winChance = (bestWinRate + 1.0) / 2.0;
        return new Move(best.targetHexagon(), winChance);
    }
}
