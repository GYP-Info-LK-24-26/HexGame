package de.hexgame.nn;

import de.hexgame.logic.Move;

import java.util.*;

import static de.hexgame.logic.GameState.BOARD_SIZE;

public class MoveOrderer {
    private final Move[] killerMoves = new Move[20];
    private final int[] history = new int[BOARD_SIZE * BOARD_SIZE];

    public void addKillerMove(int plyFromRoot, Move move) {
        killerMoves[plyFromRoot] = move;
    }

    public void addHistoryBonus(Move move, int depth) {
        history[move.getIndex()] += depth * depth;
    }

    public void orderMoves(List<Move> moves, Move hashMove, int ply) {
        moves.sort((a, b) -> Integer.compare(score(b, hashMove, ply), score(a, hashMove, ply)));
    }

    private int score(Move m, Move hashMove, int ply) {
        if (m.equals(hashMove)) {
            return 1_000_000;
        }

        if (ply < killerMoves.length && m.equals(killerMoves[ply])) {
            return 500_000;
        }

        return history[m.targetHexagon().getIndex()];
    }

    public void clear() {
        System.out.println(Arrays.toString(history));
        Arrays.fill(killerMoves, null);
        Arrays.fill(history, 0);
    }
}
