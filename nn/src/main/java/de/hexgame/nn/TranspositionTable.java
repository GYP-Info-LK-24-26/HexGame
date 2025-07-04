package de.hexgame.nn;

import de.hexgame.logic.Move;

import java.util.Arrays;

public class TranspositionTable {
    /* Entry flags á la NegaMax */
    public static final int EXACT = 0;   // exact score
    public static final int LOWER = 1;   // lower bound (fail‑high)
    public static final int UPPER = 2;   // upper bound (fail‑low)

    /** One table cell. */
    public static final class Entry {
        final long key;            // board.hashCode()
        final int depth;          // search depth in plies
        final int value;          // score from the perspective of the side to move
        final int flag;           // EXACT, LOWER, UPPER
        final Move bestMove;      // principal variation move that yielded value

        Entry(long key, int depth, int value, int flag, Move bestMove) {
            this.key      = key;
            this.depth    = depth;
            this.value    = value;
            this.flag     = flag;
            this.bestMove = bestMove;
        }
    }

    private final Entry[] table;      // buckets (power‑of‑two size)
    private final int mask;           // table.length‑1 → faster than %

    /**
     * @param size number of buckets (must be a power of two).  1 048 576
     *             (2^20) entries ≈ 32 MB is a typical sweet spot.
     */
    public TranspositionTable(int size) {
        if (Integer.bitCount(size) != 1)
            throw new IllegalArgumentException("size must be a power of two");
        table = new Entry[size];
        mask  = size - 1;
    }

    /** default 1,048,576 entries (2^20) */
    public TranspositionTable() { this(1 << 21); }

    /** Clears all buckets – call once between games. */
    public void clear() { Arrays.fill(table, null); }

    public static long stores, overwrites, accesses, misses = 0;
    /**
     * Look up a position.
     * @return the stored entry or {@code null} if unknown.
     */
    public Entry probe(long key) {
        accesses++;
        Entry e = table[(int)key & mask];
        if (e != null && e.key == key) {
            return e;
        } else {
            misses++;
            return null;
        }
    }

    /**
     * Store (replace only when new depth ≥ old depth).
     */
    public void store(long key, int depth, int value, int flag, Move bestMove) {
        stores++;
        int idx = (int)key & mask;
        Entry old = table[idx];
        table[idx] = new Entry(key, depth, value, flag, bestMove);
        if (old != null && key != old.key) {
            overwrites++;
        }
    }
}
