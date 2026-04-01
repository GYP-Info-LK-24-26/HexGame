package de.hexgame.algorithm.mcts;

import de.hexgame.logic.GameState;
import de.hexgame.logic.UnionFind;

import static de.hexgame.logic.GameState.*;

/**
 * Inferior Cell Engine (ICE) — detects dead and captured cells on a Hex board.
 * Dead cells can never affect the game outcome and can be pruned from MCTS expansion.
 * Captured cells are virtually owned by one color and can be filled in.
 * <p>
 * Detection methods run in a fixpoint loop:
 * 1. Edge-unreachable dead regions
 * 2. Group-contraction dead regions
 * 3. Simplicial dead cell detection
 * <p>
 * Per-color dead tracking derives captured cells: a cell dead for one color only
 * is captured by the other (in Hex, extra stones are never harmful).
 * After the fixpoint, captured cells are filled and the loop re-runs (cascading).
 * A post-fixpoint pass detects vulnerable/dominated cells via 1-ply lookahead.
 */
public class InferiorCellEngine {

    /**
     * Full ICE computation returning dead cells, captured cells, and the filled state.
     */
    public static ICEResult computeICE(GameState state) {
        boolean[] deadForRed = new boolean[TOTAL_CELLS];
        boolean[] deadForBlue = new boolean[TOTAL_CELLS];
        boolean[] dead = new boolean[TOTAL_CELLS];
        boolean[] capturedByRed = new boolean[TOTAL_CELLS];
        boolean[] capturedByBlue = new boolean[TOTAL_CELLS];

        GameState workingState = state.clone();

        for (int cascade = 0; cascade < 5; cascade++) {
            // Reset per-color dead (globally dead stays)
            for (int i = 0; i < TOTAL_CELLS; i++) {
                deadForRed[i] = dead[i];
                deadForBlue[i] = dead[i];
            }

            UnionFind stoneGroups = buildStoneGroups(workingState);
            int[] bfsQueue = new int[TOTAL_CELLS];

            boolean changed = true;
            while (changed) {
                changed = detectEdgeUnreachable(workingState, dead, deadForRed, deadForBlue, bfsQueue);
                changed |= detectGroupContraction(workingState, dead, deadForRed, deadForBlue, stoneGroups, bfsQueue);
                changed |= detectSimplicial(workingState, dead, deadForRed, deadForBlue, stoneGroups);
            }

            // Derive captured from per-color dead
            boolean filled = false;
            for (int i = 0; i < TOTAL_CELLS; i++) {
                if (workingState.getPiece(i) != NO_PIECE || dead[i]) continue;
                if (deadForBlue[i] && !deadForRed[i] && !capturedByRed[i]) {
                    capturedByRed[i] = true;
                }
                if (deadForRed[i] && !deadForBlue[i] && !capturedByBlue[i]) {
                    capturedByBlue[i] = true;
                }
            }

            // Resolve conflicts: captured by both → dead
            for (int i = 0; i < TOTAL_CELLS; i++) {
                if (capturedByRed[i] && capturedByBlue[i]) {
                    dead[i] = true;
                    capturedByRed[i] = false;
                    capturedByBlue[i] = false;
                }
            }

            // Fill captured cells
            for (int i = 0; i < TOTAL_CELLS; i++) {
                if (workingState.getPiece(i) != NO_PIECE) continue;
                if (capturedByRed[i]) {
                    workingState.setPiece(i, RED);
                    filled = true;
                } else if (capturedByBlue[i]) {
                    workingState.setPiece(i, BLUE);
                    filled = true;
                }
            }

            if (!filled || workingState.isFinished()) break;
        }

        // Post-fixpoint: vulnerable cell detection
        detectVulnerable(workingState, dead, deadForRed, deadForBlue);

        ICEResult result = new ICEResult();
        System.arraycopy(dead, 0, result.dead, 0, TOTAL_CELLS);
        System.arraycopy(capturedByRed, 0, result.capturedByRed, 0, TOTAL_CELLS);
        System.arraycopy(capturedByBlue, 0, result.capturedByBlue, 0, TOTAL_CELLS);
        result.filledState = workingState;
        return result;
    }

    /** Backward-compatible wrapper. */
    static boolean[] computeDeadCells(GameState state) {
        return computeICE(state).dead;
    }

    // ---- Stone group building ----

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

    // ---- Method 1: Edge-unreachable dead regions ----

    private static boolean detectEdgeUnreachable(GameState state, boolean[] dead,
                                                  boolean[] deadForRed, boolean[] deadForBlue, int[] bfsQueue) {
        boolean[] reachRedLow = bfsFromEdge(state, dead, null, RED, true, bfsQueue);
        boolean[] reachRedHigh = bfsFromEdge(state, dead, null, RED, false, bfsQueue);
        boolean[] reachBlueLow = bfsFromEdge(state, dead, null, BLUE, true, bfsQueue);
        boolean[] reachBlueHigh = bfsFromEdge(state, dead, null, BLUE, false, bfsQueue);

        boolean changed = false;
        for (int i = 0; i < TOTAL_CELLS; i++) {
            if (dead[i] || state.getPiece(i) != NO_PIECE) continue;
            boolean dfr = !reachRedLow[i] || !reachRedHigh[i];
            boolean dfb = !reachBlueLow[i] || !reachBlueHigh[i];
            if (dfr && !deadForRed[i]) { deadForRed[i] = true; changed = true; }
            if (dfb && !deadForBlue[i]) { deadForBlue[i] = true; changed = true; }
            if (dfr && dfb && !dead[i]) { dead[i] = true; changed = true; }
        }
        return changed;
    }

    // ---- Method 2: Group-contraction dead regions ----

    private static boolean detectGroupContraction(GameState state, boolean[] dead,
                                                    boolean[] deadForRed, boolean[] deadForBlue,
                                                    UnionFind stoneGroups, int[] bfsQueue) {
        boolean changed = false;
        boolean[] processedRoot = new boolean[GROUP_COUNT];

        for (int i = 0; i < TOTAL_CELLS; i++) {
            if (state.getPiece(i) == NO_PIECE || dead[i]) continue;
            int root = stoneGroups.find(i);
            if (processedRoot[root]) continue;
            processedRoot[root] = true;

            int groupColor = state.getPiece(i);
            boolean[] cutset = collectCutset(state, dead, stoneGroups, root);
            if (cutset == null) continue;

            changed |= markDeadWithGroupCutset(state, dead, deadForRed, deadForBlue, cutset, groupColor, bfsQueue);
        }
        return changed;
    }

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

    private static boolean markDeadWithGroupCutset(GameState state, boolean[] dead,
                                                    boolean[] deadForRed, boolean[] deadForBlue,
                                                    boolean[] cutset, int groupColor, int[] bfsQueue) {
        int otherColor = COLOR_SUM - groupColor;
        boolean[] reachGroupLow = bfsFromEdge(state, dead, cutset, groupColor, true, bfsQueue);
        boolean[] reachGroupHigh = bfsFromEdge(state, dead, cutset, groupColor, false, bfsQueue);
        boolean[] reachOtherLow = bfsFromEdge(state, dead, null, otherColor, true, bfsQueue);
        boolean[] reachOtherHigh = bfsFromEdge(state, dead, null, otherColor, false, bfsQueue);

        boolean[] dfrGroup = groupColor == RED ? deadForRed : deadForBlue;
        boolean[] dfrOther = groupColor == RED ? deadForBlue : deadForRed;

        boolean changed = false;
        for (int i = 0; i < TOTAL_CELLS; i++) {
            if (dead[i] || state.getPiece(i) != NO_PIECE) continue;
            if (cutset[i]) continue;
            boolean deadForGroup = !reachGroupLow[i] || !reachGroupHigh[i];
            boolean deadForOtherC = !reachOtherLow[i] || !reachOtherHigh[i];
            if (deadForGroup && !dfrGroup[i]) { dfrGroup[i] = true; changed = true; }
            if (deadForOtherC && !dfrOther[i]) { dfrOther[i] = true; changed = true; }
            if (deadForGroup && deadForOtherC && !dead[i]) { dead[i] = true; changed = true; }
        }
        return changed;
    }

    // ---- BFS infrastructure ----

    private static boolean[] bfsFromEdge(GameState state, boolean[] dead, boolean[] excluded,
                                          int color, boolean lowEdge, int[] bfsQueue) {
        boolean[] visited = new boolean[TOTAL_CELLS];
        int head = 0, tail = 0;
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

    private static boolean canTraverse(GameState state, int cellIndex, int color) {
        int piece = state.getPiece(cellIndex);
        return piece == NO_PIECE || piece == color;
    }

    private static boolean isExcluded(boolean[] excluded, int cellIndex) {
        return excluded != null && excluded[cellIndex];
    }

    private static int edgeCellIndex(int color, boolean lowEdge, int i) {
        if (color == RED) {
            return lowEdge ? i * BOARD_SIZE : i * BOARD_SIZE + (BOARD_SIZE - 1);
        } else {
            return lowEdge ? i : (BOARD_SIZE - 1) * BOARD_SIZE + i;
        }
    }

    // ---- Method 3: Simplicial dead cell detection ----

    private static boolean detectSimplicial(GameState state, boolean[] dead,
                                             boolean[] deadForRed, boolean[] deadForBlue,
                                             UnionFind stoneGroups) {
        boolean changed = false;
        int[] neighborGroupIds = new int[8];
        for (int i = 0; i < TOTAL_CELLS; i++) {
            if (dead[i] || state.getPiece(i) != NO_PIECE) continue;
            boolean simpRed = isSimplicialForColor(state, dead, stoneGroups, i, RED, neighborGroupIds);
            boolean simpBlue = isSimplicialForColor(state, dead, stoneGroups, i, BLUE, neighborGroupIds);
            if (simpRed && !deadForRed[i]) { deadForRed[i] = true; changed = true; }
            if (simpBlue && !deadForBlue[i]) { deadForBlue[i] = true; changed = true; }
            if (simpRed && simpBlue && !dead[i]) { dead[i] = true; changed = true; }
        }
        return changed;
    }

    private static boolean isSimplicialForColor(GameState state, boolean[] dead, UnionFind stoneGroups,
                                                  int cellIndex, int color, int[] neighborGroupIds) {
        int count = collectNeighborGroupsForColor(state, dead, stoneGroups, cellIndex, color, neighborGroupIds);
        if (count <= 1) return true;
        return isCliqueForColor(state, dead, stoneGroups, neighborGroupIds, count, cellIndex, color);
    }

    private static int collectNeighborGroupsForColor(GameState state, boolean[] dead, UnionFind stoneGroups,
                                                       int cellIndex, int color, int[] neighborGroupIds) {
        int count = 0;
        for (int neighbor : NEIGHBORS[cellIndex]) {
            if (dead[neighbor]) continue;
            int piece = state.getPiece(neighbor);
            int groupId;
            if (piece == color) {
                groupId = stoneGroups.find(neighbor);
            } else if (piece == NO_PIECE) {
                groupId = neighbor;
            } else {
                continue;
            }
            if (!containsGroup(neighborGroupIds, count, groupId)) {
                neighborGroupIds[count++] = groupId;
            }
        }
        int row = cellIndex / BOARD_SIZE;
        int col = cellIndex % BOARD_SIZE;
        if (color == RED) {
            if (col == 0) {
                int edgeGroup = stoneGroups.find(EDGE_RED_LOW);
                if (!containsGroup(neighborGroupIds, count, edgeGroup))
                    neighborGroupIds[count++] = edgeGroup;
            }
            if (col == BOARD_SIZE - 1) {
                int edgeGroup = stoneGroups.find(EDGE_RED_HIGH);
                if (!containsGroup(neighborGroupIds, count, edgeGroup))
                    neighborGroupIds[count++] = edgeGroup;
            }
        } else {
            if (row == 0) {
                int edgeGroup = stoneGroups.find(EDGE_BLUE_LOW);
                if (!containsGroup(neighborGroupIds, count, edgeGroup))
                    neighborGroupIds[count++] = edgeGroup;
            }
            if (row == BOARD_SIZE - 1) {
                int edgeGroup = stoneGroups.find(EDGE_BLUE_HIGH);
                if (!containsGroup(neighborGroupIds, count, edgeGroup))
                    neighborGroupIds[count++] = edgeGroup;
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

    private static boolean isCliqueForColor(GameState state, boolean[] dead, UnionFind stoneGroups,
                                              int[] groupIds, int groupCount, int candidateCell, int color) {
        for (int a = 0; a < groupCount; a++) {
            for (int b = a + 1; b < groupCount; b++) {
                if (!areGroupsAdjacentForColor(state, dead, stoneGroups,
                        groupIds[a], groupIds[b], candidateCell, color)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean areGroupsAdjacentForColor(GameState state, boolean[] dead, UnionFind stoneGroups,
                                                       int groupA, int groupB, int candidateCell, int color) {
        if (groupA >= TOTAL_CELLS && groupB >= TOTAL_CELLS) {
            return false;
        }
        if (groupA >= TOTAL_CELLS) {
            return isStandaloneEdgeAdjacentToGroup(groupA, groupB, candidateCell, state, dead);
        }
        if (groupB >= TOTAL_CELLS) {
            return isStandaloneEdgeAdjacentToGroup(groupB, groupA, candidateCell, state, dead);
        }
        for (int neighbor : NEIGHBORS[candidateCell]) {
            if (dead[neighbor]) continue;
            int piece = state.getPiece(neighbor);
            int neighborGroup;
            if (piece == color) {
                neighborGroup = stoneGroups.find(neighbor);
            } else if (piece == NO_PIECE) {
                neighborGroup = neighbor;
            } else {
                continue;
            }
            if (neighborGroup != groupA) continue;
            if (isAdjacentToGroupForColor(state, dead, stoneGroups, neighbor, groupA, groupB, candidateCell, color)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStandaloneEdgeAdjacentToGroup(int edgeNode, int otherGroup, int candidateCell,
                                                             GameState state, boolean[] dead) {
        if (otherGroup >= TOTAL_CELLS) return false;
        if (state.getPiece(otherGroup) != NO_PIECE) return false;
        return otherGroup != candidateCell && !dead[otherGroup] && isOnEdge(otherGroup, edgeNode);
    }

    private static boolean isOnEdge(int cellIndex, int edgeNode) {
        if (edgeNode == EDGE_RED_LOW) return cellIndex % BOARD_SIZE == 0;
        if (edgeNode == EDGE_RED_HIGH) return cellIndex % BOARD_SIZE == BOARD_SIZE - 1;
        if (edgeNode == EDGE_BLUE_LOW) return cellIndex / BOARD_SIZE == 0;
        if (edgeNode == EDGE_BLUE_HIGH) return cellIndex / BOARD_SIZE == BOARD_SIZE - 1;
        return false;
    }

    private static boolean isAdjacentToGroupForColor(GameState state, boolean[] dead, UnionFind stoneGroups,
                                                       int startCell, int groupA, int groupB,
                                                       int candidateCell, int color) {
        if (state.getPiece(startCell) == NO_PIECE) {
            return hasNeighborInGroupForColor(state, dead, stoneGroups, startCell, groupB, candidateCell, color);
        }
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
                if (piece == color) {
                    int neighborGroup = stoneGroups.find(neighbor);
                    if (neighborGroup == groupB) return true;
                    if (neighborGroup == groupA && !visited[neighbor]) {
                        visited[neighbor] = true;
                        stack[top++] = neighbor;
                    }
                } else if (piece == NO_PIECE) {
                    if (neighbor == groupB) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasNeighborInGroupForColor(GameState state, boolean[] dead, UnionFind stoneGroups,
                                                        int cellIndex, int targetGroup, int candidateCell, int color) {
        for (int neighbor : NEIGHBORS[cellIndex]) {
            if (neighbor == candidateCell || dead[neighbor]) continue;
            int piece = state.getPiece(neighbor);
            if (piece == color) {
                if (stoneGroups.find(neighbor) == targetGroup) return true;
            } else if (piece == NO_PIECE) {
                if (neighbor == targetGroup) return true;
            }
        }
        return false;
    }

    // ---- Post-fixpoint: Vulnerable cell detection ----

    private static void detectVulnerable(GameState state, boolean[] dead,
                                          boolean[] deadForRed, boolean[] deadForBlue) {
        int[] bfsQueue = new int[TOTAL_CELLS];

        for (int v = 0; v < TOTAL_CELLS; v++) {
            if (dead[v] || state.getPiece(v) != NO_PIECE) continue;

            // Only check cells with few empty neighbors for efficiency
            int emptyNeighborCount = 0;
            for (int n : NEIGHBORS[v]) {
                if (state.getPiece(n) == NO_PIECE && !dead[n]) emptyNeighborCount++;
            }
            if (emptyNeighborCount > 3) continue;

            boolean vulnerableForRed = deadForRed[v];
            boolean vulnerableForBlue = deadForBlue[v];
            if (vulnerableForRed && vulnerableForBlue) {
                dead[v] = true;
                continue;
            }

            for (int n : NEIGHBORS[v]) {
                if (dead[n] || state.getPiece(n) != NO_PIECE) continue;
                if (vulnerableForRed && vulnerableForBlue) break;

                // Check if placing opponent at n makes v dead for RED
                if (!vulnerableForRed) {
                    if (isDeadAfterPlacement(state, dead, v, n, RED, bfsQueue)) {
                        vulnerableForRed = true;
                    }
                }
                // Check if placing opponent at n makes v dead for BLUE
                if (!vulnerableForBlue) {
                    if (isDeadAfterPlacement(state, dead, v, n, BLUE, bfsQueue)) {
                        vulnerableForBlue = true;
                    }
                }
            }

            // Dominated: vulnerable for both colors → dead
            if (vulnerableForRed && vulnerableForBlue) {
                dead[v] = true;
                deadForRed[v] = true;
                deadForBlue[v] = true;
            }
        }
    }

    /**
     * Checks if targetCell becomes dead for checkColor when killerCell is blocked
     * (simulating the opponent placing a stone there).
     */
    private static boolean isDeadAfterPlacement(GameState state, boolean[] dead,
                                                 int targetCell, int killerCell,
                                                 int checkColor, int[] bfsQueue) {
        boolean[] tempDead = new boolean[TOTAL_CELLS];
        System.arraycopy(dead, 0, tempDead, 0, TOTAL_CELLS);
        tempDead[killerCell] = true;

        boolean[] reachLow = bfsFromEdge(state, tempDead, null, checkColor, true, bfsQueue);
        boolean[] reachHigh = bfsFromEdge(state, tempDead, null, checkColor, false, bfsQueue);

        return !reachLow[targetCell] || !reachHigh[targetCell];
    }
}
