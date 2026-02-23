package de.hexgame.algorithm.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Piece;
import de.hexgame.logic.Position;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static de.hexgame.logic.GameState.BOARD_SIZE;

public class TreeNodeExp {
    private static final float EXPLORATION_FACTOR = 1.4f;
    private static final float RAVE_K = 300f;
    private static final float GIBBS_TEMPERATURE = 0.3f;
    private static final int TOTAL_CELLS = BOARD_SIZE * BOARD_SIZE;

    private TreeNodeExp parent;
    private final List<TreeNodeExp> children = new ArrayList<>();

    private final Move move;
    private GameState gameState; // lazily materialized

    @Getter
    private int visits = 0;
    private float valueSum = 0.0f;

    // References to global RAVE table (owned by GameTree)
    // Flat 1D arrays indexed by [colorOrdinal * TOTAL_CELLS + cellIndex]
    private final float[] raveValueSum;
    private final int[] raveVisitCount;

    private record RolloutResult(float value, boolean[] redMoves, boolean[] blueMoves) {}

    public TreeNodeExp(TreeNodeExp parent, Move move, GameState gameState,
                       float[] raveValueSum, int[] raveVisitCount) {
        this.parent = parent;
        this.move = move;
        this.gameState = gameState;
        this.raveValueSum = raveValueSum;
        this.raveVisitCount = raveVisitCount;
    }

    public float getMeanValue() {
        return visits == 0 ? 0.0f : valueSum / visits;
    }

    public TreeNodeExp jumpTo(GameState gameState) {
        if (this.gameState.getHalfMoveCounter() > gameState.getHalfMoveCounter()) {
            return null;
        }

        if (this.gameState.equals(gameState)) {
            parent = null;
            return this;
        }

        for (TreeNodeExp child : children) {
            TreeNodeExp target = child.jumpTo(gameState);
            if (target != null) {
                return target;
            }
        }

        return null;
    }

    public void simulate() {
        // Selection: walk down tree using UCT
        TreeNodeExp node = this;
        while (!node.children.isEmpty()) {
            node = node.selectChild();
        }

        // Lazy state materialization
        if (node.visits == 0 && node.move != null) {
            node.gameState = node.gameState.clone();
            node.gameState.makeMove(node.move);
        }

        // Terminal node
        if (node.gameState.isFinished()) {
            node.backpropagate(-1.0f);
            return;
        }

        // Expansion: if node has been visited before, expand it
        if (node.visits > 0) {
            for (Move legalMove : node.gameState.getLegalMoves()) {
                node.children.add(new TreeNodeExp(node, legalMove, node.gameState,
                        raveValueSum, raveVisitCount));
            }
            node = node.selectChild();
            // Materialize state for the new child
            node.gameState = node.gameState.clone();
            node.gameState.makeMove(node.move);

            if (node.gameState.isFinished()) {
                node.backpropagate(-1.0f);
                return;
            }
        }

        // Rollout
        RolloutResult result = rollout(node.gameState);

        // Update global RAVE table with rollout moves
        // value is from node's sideToMove perspective before rollout
        // redValue/blueValue are from each color's own perspective
        float redValue;
        float blueValue;
        Piece.Color nodeSide = node.gameState.getSideToMove();
        if (nodeSide == Piece.Color.RED) {
            redValue = result.value();   // value is from RED's perspective
            blueValue = -result.value();
        } else {
            blueValue = result.value();  // value is from BLUE's perspective
            redValue = -result.value();
        }

        for (int i = 0; i < TOTAL_CELLS; i++) {
            if (result.redMoves()[i]) {
                raveVisitCount[i]++;
                raveValueSum[i] += redValue;
            }
            if (result.blueMoves()[i]) {
                raveVisitCount[TOTAL_CELLS + i]++;
                raveValueSum[TOTAL_CELLS + i] += blueValue;
            }
        }

        node.backpropagate(result.value());
    }

    private TreeNodeExp selectChild() {
        float bestValue = Float.NEGATIVE_INFINITY;
        TreeNodeExp best = null;
        double logParentVisits = Math.log(visits);
        Piece.Color sideToMove = gameState.getSideToMove();
        int colorIdx = sideToMove.ordinal();

        for (TreeNodeExp child : children) {
            float value;
            int cellIdx = child.move.targetHexagon().row() * BOARD_SIZE + child.move.targetHexagon().column();

            int raveIdx = colorIdx * TOTAL_CELLS + cellIdx;
            if (child.visits == 0) {
                // Unvisited: always prioritize over visited nodes (FPU).
                // Rank among themselves by global RAVE value if available, random otherwise.
                float tieBreaker = raveVisitCount[raveIdx] > 0
                        ? raveValueSum[raveIdx] / raveVisitCount[raveIdx]
                        : -ThreadLocalRandom.current().nextFloat();
                value = Float.MAX_VALUE + tieBreaker;
            } else {
                float mcValue = -child.getMeanValue();

                float globalRave = raveVisitCount[raveIdx] > 0
                        ? raveValueSum[raveIdx] / raveVisitCount[raveIdx]
                        : mcValue;

                float beta = (float) Math.sqrt(RAVE_K / (3.0 * child.visits + RAVE_K));
                float exploration = (float) (EXPLORATION_FACTOR * Math.sqrt(logParentVisits / child.visits));

                value = (1 - beta) * mcValue + beta * globalRave + exploration;
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
        float[] weights = new float[TOTAL_CELLS];

        GameState sim = state.clone();
        while (!sim.isFinished()) {
            Piece.Color side = sim.getSideToMove();
            int raveOffset = side.ordinal() * TOTAL_CELLS;

            // First pass: find max score for numerical stability
            float maxScore = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < TOTAL_CELLS; i++) {
                if (sim.getPiece(i) == null) {
                    int ri = raveOffset + i;
                    if (raveVisitCount[ri] > 0) {
                        float score = raveValueSum[ri] / raveVisitCount[ri];
                        if (score > maxScore) {
                            maxScore = score;
                        }
                    }
                }
            }
            if (maxScore == Float.NEGATIVE_INFINITY) {
                maxScore = 0;
            }

            // Second pass: compute Gibbs weights and sample
            float totalWeight = 0;
            for (int i = 0; i < TOTAL_CELLS; i++) {
                if (sim.getPiece(i) == null) {
                    int ri = raveOffset + i;
                    float score = raveVisitCount[ri] > 0
                            ? raveValueSum[ri] / raveVisitCount[ri]
                            : 0;
                    weights[i] = (float) Math.exp((score - maxScore) / GIBBS_TEMPERATURE);
                    totalWeight += weights[i];
                } else {
                    weights[i] = 0;
                }
            }

            // Sample proportionally
            float r = ThreadLocalRandom.current().nextFloat() * totalWeight;
            float cumulative = 0;
            int chosen = -1;
            for (int i = 0; i < TOTAL_CELLS; i++) {
                if (weights[i] > 0) {
                    cumulative += weights[i];
                    if (cumulative >= r) {
                        chosen = i;
                        break;
                    }
                }
            }
            // Fallback (floating point edge case)
            if (chosen == -1) {
                for (int i = TOTAL_CELLS - 1; i >= 0; i--) {
                    if (weights[i] > 0) {
                        chosen = i;
                        break;
                    }
                }
            }

            Move m = new Move(new Position(chosen / BOARD_SIZE, chosen % BOARD_SIZE));
            if (side == Piece.Color.RED) {
                redMoves[chosen] = true;
            } else {
                blueMoves[chosen] = true;
            }
            sim.makeMove(m);
        }

        // sim.getSideToMove() is the LOSING side (switched after winning move).
        float value = state.getSideToMove() == sim.getSideToMove() ? -1.0f : 1.0f;
        return new RolloutResult(value, redMoves, blueMoves);
    }

    private void backpropagate(float eval) {
        visits++;
        valueSum += eval;

        if (parent != null) {
            parent.backpropagate(-eval);
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
            sb.append(String.format("  n%d [label=\"(%d,%d)|v:%d w:%.0f%%\", fillcolor=\"%s\"];\n",
                    id, move.targetHexagon().row(), move.targetHexagon().column(),
                    visits, winRate * 100, color));
        }

        if (depth < maxDepth) {
            for (TreeNodeExp child : children) {
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

        for (TreeNodeExp child : children) {
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
