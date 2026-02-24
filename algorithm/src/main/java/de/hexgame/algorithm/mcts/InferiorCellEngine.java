package de.hexgame.algorithm.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.logic.UnionFind;

import static de.hexgame.logic.GameState.*;

/**
 * Inferior Cell Engine (ICE) — detects provably dead cells on a Hex board.
 * Dead cells can never affect the game outcome and can be pruned from MCTS expansion.
 * <p>
 * Implements three Tier-1 detection methods run in a fixpoint loop:
 * 1. Edge-unreachable dead regions
 * 2. Group-contraction dead regions
 * 3. Simplicial dead cell detection
 */
class InferiorCellEngine {
    /**
     * Computes a mask of dead (prunable) cells for the given game state.
     *
     * @return boolean array of size 121; true = cell is dead and should be pruned
     */
    static boolean[] computeDeadCells(GameState state) {
        boolean[] dead = new boolean[TOTAL_CELLS];

        UnionFind stoneGroups = buildStoneGroups(state);

        int[] bfsQueue = new int[TOTAL_CELLS];

        boolean changed = true;
        while (changed) {
            changed = detectEdgeUnreachable(state, dead, bfsQueue);
            changed |= detectGroupContraction(state, dead, stoneGroups, bfsQueue);
            changed |= detectSimplicial(state, dead, stoneGroups);
        }

        return dead;
    }

    /**
     * Builds a union-find over same-color adjacent stones, including goal-side
     * edge virtual nodes ({@link GameState#GROUP_HIGH} / {@link GameState#GROUP_LOW}).
     */
    private static UnionFind buildStoneGroups(GameState state) {
        UnionFind stoneGroups = new UnionFind(GROUP_COUNT);

        for (int i = 0; i < TOTAL_CELLS; i++) {
            int piece = state.getPiece(i);
            if (piece == NO_PIECE) continue;

            for (int neighbor : NEIGHBORS[i]) {
                if (state.getPiece(neighbor) == piece) {
                    stoneGroups.union(i, neighbor);
                }
            }

            GameState.unionWithGoalEdge(stoneGroups, i, piece);
        }

        return stoneGroups;
    }

    // --- Method 1: Edge-unreachable dead regions ---

    /**
     * For each color: BFS from low edge and high edge, traversing empty+same-color cells, skipping dead.
     * Cell unreachable from either edge for that color → dead-for-that-color.
     * Cell dead for BOTH colors → globally dead.
     */
    private static boolean detectEdgeUnreachable(GameState state, boolean[] dead, int[] bfsQueue) {
        return markDeadForBothColors(state, dead, null, bfsQueue);
    }

    // --- Method 2: Group-contraction dead regions ---

    /**
     * For each stone group: its empty neighbors form a cutset.
     * BFS from both edges for both colors, excluding the cutset.
     * Empty cells behind the cutset (unreachable from both edges for both colors) → dead.
     */
    private static boolean detectGroupContraction(GameState state, boolean[] dead, UnionFind stoneGroups, int[] bfsQueue) {
        boolean changed = false;

        boolean[] processedRoot = new boolean[GROUP_COUNT];

        for (int i = 0; i < TOTAL_CELLS; i++) {
            if (state.getPiece(i) == NO_PIECE || dead[i]) continue;

            int root = stoneGroups.find(i);
            if (processedRoot[root]) continue;
            processedRoot[root] = true;

            boolean[] cutset = collectCutset(state, dead, stoneGroups, root);
            if (cutset == null) continue;

            changed |= markDeadForBothColors(state, dead, cutset, bfsQueue);
        }

        return changed;
    }

    /**
     * Collects the cutset (empty non-dead neighbors) of the stone group with the given root.
     *
     * @return the cutset mask, or null if the cutset is empty
     */
    private static boolean[] collectCutset(GameState state, boolean[] dead, UnionFind stoneGroups, int groupRoot) {
        boolean[] cutset = new boolean[TOTAL_CELLS];
        int cutsetSize = 0;

        for (int j = 0; j < TOTAL_CELLS; j++) {
            if (state.getPiece(j) == NO_PIECE || stoneGroups.find(j) != groupRoot) continue;

            for (int neighbor : NEIGHBORS[j]) {
                if (!dead[neighbor] && state.getPiece(neighbor) == NO_PIECE && !cutset[neighbor]) {
                    cutset[neighbor] = true;
                    cutsetSize++;
                }
            }
        }

        return cutsetSize > 0 ? cutset : null;
    }

    // --- Shared BFS infrastructure for methods 1 and 2 ---

    /**
     * Runs 4 BFS passes (2 colors × 2 goal edges) and marks empty cells as dead when they are
     * unreachable from at least one goal edge for BOTH colors.
     *
     * @param excluded optional exclusion mask (e.g. a cutset); null means no exclusion
     */
    private static boolean markDeadForBothColors(GameState state, boolean[] dead, boolean[] excluded, int[] bfsQueue) {
        boolean[] reachableRedLow = bfsFromEdge(state, dead, excluded, RED, true, bfsQueue);
        boolean[] reachableRedHigh = bfsFromEdge(state, dead, excluded, RED, false, bfsQueue);
        boolean[] reachableBlueLow = bfsFromEdge(state, dead, excluded, BLUE, true, bfsQueue);
        boolean[] reachableBlueHigh = bfsFromEdge(state, dead, excluded, BLUE, false, bfsQueue);

        boolean changed = false;
        for (int i = 0; i < TOTAL_CELLS; i++) {
            if (dead[i] || state.getPiece(i) != NO_PIECE) continue;
            if (excluded != null && excluded[i]) continue;

            boolean deadForRed = !reachableRedLow[i] || !reachableRedHigh[i];
            boolean deadForBlue = !reachableBlueLow[i] || !reachableBlueHigh[i];
            if (deadForRed && deadForBlue) {
                dead[i] = true;
                changed = true;
            }
        }
        return changed;
    }

    /**
     * BFS from one goal edge for a given color, skipping dead and optionally excluded cells.
     * RED connects left (col=0) to right (col=10), so low=col0, high=col10.
     * BLUE connects top (row=0) to bottom (row=10), so low=row0, high=row10.
     *
     * @param excluded optional exclusion mask; null means no exclusion
     */
    private static boolean[] bfsFromEdge(GameState state, boolean[] dead, boolean[] excluded,
                                          int color, boolean lowEdge, int[] bfsQueue) {
        boolean[] visited = new boolean[TOTAL_CELLS];
        int head = 0;
        int tail = 0;

        for (int i = 0; i < BOARD_SIZE; i++) {
            int edgeCell = edgeCellIndex(color, lowEdge, i);
            if (!dead[edgeCell] && !isExcluded(excluded, edgeCell) && canTraverse(state, edgeCell, color)) {
                visited[edgeCell] = true;
                bfsQueue[tail++] = edgeCell;
            }
        }

        while (head < tail) {
            int current = bfsQueue[head++];
            for (int neighbor : NEIGHBORS[current]) {
                if (!visited[neighbor] && !dead[neighbor] && !isExcluded(excluded, neighbor)
                        && canTraverse(state, neighbor, color)) {
                    visited[neighbor] = true;
                    bfsQueue[tail++] = neighbor;
                }
            }
        }

        return visited;
    }

    /** A cell is traversable for a color if it's empty or occupied by that color. */
    private static boolean canTraverse(GameState state, int cellIndex, int color) {
        int piece = state.getPiece(cellIndex);
        return piece == NO_PIECE || piece == color;
    }

    private static boolean isExcluded(boolean[] excluded, int cellIndex) {
        return excluded != null && excluded[cellIndex];
    }

    /**
     * Returns the board index of the i-th cell along the specified edge.
     * RED low = column 0, RED high = column BOARD_SIZE-1.
     * BLUE low = row 0, BLUE high = row BOARD_SIZE-1.
     */
    private static int edgeCellIndex(int color, boolean lowEdge, int i) {
        if (color == RED) {
            return lowEdge ? i * BOARD_SIZE : i * BOARD_SIZE + (BOARD_SIZE - 1);
        } else {
            return lowEdge ? i : (BOARD_SIZE - 1) * BOARD_SIZE + i;
        }
    }

    // --- Method 3: Simplicial dead cell detection ---

    /**
     * For each empty cell, build its neighbor set (contracting stone groups to single nodes, ignoring dead cells).
     * If ≤1 distinct neighbor → trivially dead.
     * If all pairs of distinct neighbors are adjacent → dead (clique check).
     */
    private static boolean detectSimplicial(GameState state, boolean[] dead, UnionFind stoneGroups) {
        boolean changed = false;

        // 6 hex neighbors + up to 2 edge virtual nodes
        int[] neighborGroupIds = new int[8];

        for (int i = 0; i < TOTAL_CELLS; i++) {
            if (dead[i] || state.getPiece(i) != NO_PIECE) continue;

            int neighborGroupCount = collectNeighborGroups(state, dead, stoneGroups, i, neighborGroupIds);

            if (neighborGroupCount <= 1) {
                dead[i] = true;
                changed = true;
                continue;
            }

            if (isClique(state, dead, stoneGroups, neighborGroupIds, neighborGroupCount, i)) {
                dead[i] = true;
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Collects the distinct neighbor group IDs for the given empty cell,
     * including goal-side edge virtual nodes for cells on the board edges.
     *
     * @return the number of distinct neighbor groups found
     */
    private static int collectNeighborGroups(GameState state, boolean[] dead, UnionFind stoneGroups,
                                              int cellIndex, int[] neighborGroupIds) {
        int count = 0;

        for (int neighbor : NEIGHBORS[cellIndex]) {
            if (dead[neighbor]) continue;

            int groupId;
            if (state.getPiece(neighbor) != NO_PIECE) {
                groupId = stoneGroups.find(neighbor);
            } else {
                groupId = neighbor; // empty cells are their own group
            }

            if (!containsGroup(neighborGroupIds, count, groupId)) {
                neighborGroupIds[count++] = groupId;
            }
        }

        // Add goal-side edge virtual nodes as neighbors for cells on the board edges
        int row = cellIndex / BOARD_SIZE;
        int column = cellIndex % BOARD_SIZE;
        if (column == 0 || row == 0) {
            int lowEdgeGroup = stoneGroups.find(GROUP_LOW);
            if (!containsGroup(neighborGroupIds, count, lowEdgeGroup)) {
                neighborGroupIds[count++] = lowEdgeGroup;
            }
        }
        if (column == BOARD_SIZE - 1 || row == BOARD_SIZE - 1) {
            int highEdgeGroup = stoneGroups.find(GROUP_HIGH);
            if (!containsGroup(neighborGroupIds, count, highEdgeGroup)) {
                neighborGroupIds[count++] = highEdgeGroup;
            }
        }

        return count;
    }

    private static boolean containsGroup(int[] groupIds, int count, int groupId) {
        for (int i = 0; i < count; i++) {
            if (groupIds[i] == groupId) return true;
        }
        return false;
    }

    /**
     * Checks if the neighbor groups of cell {@code candidateCell} form a clique.
     * Two groups are "adjacent" if any member of one group is a hex neighbor of any member of the other group
     * (excluding the candidate cell itself).
     */
    private static boolean isClique(GameState state, boolean[] dead, UnionFind stoneGroups,
                                     int[] groupIds, int groupCount, int candidateCell) {
        for (int a = 0; a < groupCount; a++) {
            for (int b = a + 1; b < groupCount; b++) {
                if (!areGroupsAdjacent(state, dead, stoneGroups, groupIds[a], groupIds[b], candidateCell)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Two groups are adjacent if any cell belonging to group A is a hex neighbor of any cell belonging to group B,
     * where the connecting neighbor is not the candidate cell being tested.
     * <p>
     * Searches by iterating hex neighbors of the candidate cell that belong to groupA, then following
     * same-group links to check for adjacency to groupB.
     */
    private static boolean areGroupsAdjacent(GameState state, boolean[] dead, UnionFind stoneGroups,
                                              int groupA, int groupB, int candidateCell) {
        for (int neighbor : NEIGHBORS[candidateCell]) {
            if (dead[neighbor]) continue;

            int neighborGroup = state.getPiece(neighbor) != NO_PIECE
                    ? stoneGroups.find(neighbor)
                    : neighbor;
            if (neighborGroup != groupA) continue;

            if (isAdjacentToGroup(state, dead, stoneGroups, neighbor, groupA, groupB, candidateCell)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Starting from a cell in groupA, checks if groupA is adjacent to groupB
     * by examining neighbors of all groupA cells reachable from {@code startCell}.
     */
    private static boolean isAdjacentToGroup(GameState state, boolean[] dead, UnionFind stoneGroups,
                                              int startCell, int groupA, int groupB, int candidateCell) {
        if (state.getPiece(startCell) == NO_PIECE) {
            return hasNeighborInGroup(state, dead, stoneGroups, startCell, groupB, candidateCell);
        }

        // BFS through the stone group to find any cell adjacent to groupB
        boolean[] visited = new boolean[TOTAL_CELLS];
        int[] stack = new int[TOTAL_CELLS];
        int top = 0;
        stack[top++] = startCell;
        visited[startCell] = true;

        while (top > 0) {
            int current = stack[--top];
            for (int neighbor : NEIGHBORS[current]) {
                if (neighbor == candidateCell || dead[neighbor]) continue;

                int piece = state.getPiece(neighbor);
                if (piece != NO_PIECE) {
                    int neighborGroup = stoneGroups.find(neighbor);
                    if (neighborGroup == groupB) return true;
                    if (neighborGroup == groupA && !visited[neighbor]) {
                        visited[neighbor] = true;
                        stack[top++] = neighbor;
                    }
                } else {
                    if (neighbor == groupB) return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks whether any hex neighbor of the given cell belongs to the target group.
     */
    private static boolean hasNeighborInGroup(GameState state, boolean[] dead, UnionFind stoneGroups,
                                               int cellIndex, int targetGroup, int candidateCell) {
        for (int neighbor : NEIGHBORS[cellIndex]) {
            if (neighbor == candidateCell || dead[neighbor]) continue;

            int neighborGroup = state.getPiece(neighbor) != NO_PIECE
                    ? stoneGroups.find(neighbor)
                    : neighbor;
            if (neighborGroup == targetGroup) return true;
        }
        return false;
    }
}
