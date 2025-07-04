package de.hexgame.nn;

import de.hexgame.logic.*;

import java.util.*;

import static de.hexgame.logic.GameState.BOARD_SIZE;

public class FinnAlgoPlayer implements Player {
    private static final long THINK_TIME = 2000;

    private static final int INF = 1000000;
    private static final int W_DIST = 800;
    private static final int W_BRID = 100;
    private static final int W_CENT = 10;
    static boolean debug = false;
    private final TranspositionTable transpositionTable = new TranspositionTable();
    private final MoveOrderer moveOrderer = new MoveOrderer();
    private long startMillis;
    private Move bestMove;
    private int bestEval;

    @Override
    public String getName() {
        return "FinnAlgoPlayer";
    }

    @Override
    public Move think(GameState gameState) {
        this.startMillis = System.currentTimeMillis();
        bestEval = 0;
        gameState = gameState.clone();
        gameState.setPlayerMoveListeners(new ArrayList<>());
        runIterativeDeepeningSearch(gameState);
        bestMove = new Move(bestMove.targetHexagon(), 0.5 + 0.5 * (2 / (1 + Math.exp(-0.002 * bestEval)) - 1));
        return bestMove;
    }

    private void runIterativeDeepeningSearch(GameState gameState) {
        for (int depth = 1; depth < 100; depth++) {
            search(gameState, depth, 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);

            if (!debug && (isWin(bestEval) || System.currentTimeMillis() - startMillis > THINK_TIME)) {
                moveOrderer.clear();
                System.out.printf("Eval: %d, Depth: %d, Stores: %d, Overwrites: %d, Accesses: %d, Misses: %d\n",
                        bestEval, depth, TranspositionTable.stores, TranspositionTable.overwrites,
                        TranspositionTable.accesses, TranspositionTable.misses);
                System.out.printf("NVS count: %d, Researches: %d\n", nvs, researches);
                nvs = researches = 0;
                TranspositionTable.stores = TranspositionTable.overwrites = TranspositionTable.accesses = TranspositionTable.misses = 0;
                return;
            }
        }
    }

    static long nvs = 0, researches = 0;

    // TODO: Dead-Cell-Pruning
    private int search(GameState gameState, int depth, int plyFromRoot, int alpha, int beta) {
        if (gameState.isFinished()) {
            return -(1000000 - plyFromRoot);
        }

        final long key = gameState.hashCodeLong();
        TranspositionTable.Entry ttEntry = transpositionTable.probe(key);
        if (ttEntry != null && ttEntry.depth >= depth) {
            switch (ttEntry.flag) {
                case TranspositionTable.EXACT:
                    if (plyFromRoot == 0) {
                        bestMove = ttEntry.bestMove;
                        bestEval = ttEntry.value;
                    }
                    return ttEntry.value;
                case TranspositionTable.LOWER:
                    alpha = Math.max(alpha, ttEntry.value);
                    break;
                case TranspositionTable.UPPER:
                    beta = Math.min(beta, ttEntry.value);
                    break;
            }
            if (alpha >= beta) {
                if (plyFromRoot == 0) {
                    bestMove = ttEntry.bestMove;
                    bestEval = ttEntry.value;
                }
                return ttEntry.value;
            }
        }

        if (depth <= 0) {
            int eval = evaluate(gameState);
            transpositionTable.store(key, 0, eval, TranspositionTable.EXACT, null);
            return eval;
        }

        final int oldAlpha = alpha;
        int bestEvalInThisPosition = Integer.MIN_VALUE;
        Move bestMoveInThisPosition = null;
        List<Move> moves = gameState.getLegalMoves();
        Move hashMove = ttEntry == null ? null : ttEntry.bestMove;

        moveOrderer.orderMoves(moves, hashMove, plyFromRoot);
        for (int i = 0; i < moves.size(); i++) {
            final Move move = moves.get(i);
            GameState newGameState = gameState.clone();
            newGameState.makeMove(move);
            int eval = 0;
            boolean needsFullSearch = true;
            if (hashMove != null && i >= 1) {
                int reduction = (int) (Math.log(depth) * Math.log(i) / 2.0);
                eval = -search(newGameState, depth - 1 - reduction, plyFromRoot + 1, -(alpha + 1), -alpha);
                needsFullSearch = eval > alpha;
                nvs++;
                if (needsFullSearch) researches++;
            }
            if (needsFullSearch) {
                eval = -search(newGameState, depth - 1, plyFromRoot + 1, -beta, -alpha);
            }
            if (System.currentTimeMillis() - startMillis > THINK_TIME * 2) {
                return 0;
            }
            if (eval > bestEvalInThisPosition) {
                bestEvalInThisPosition = eval;
                bestMoveInThisPosition = move;
                if (eval > alpha) {
                    alpha = eval;
                }
            }
            if (eval >= beta) {
                moveOrderer.addKillerMove(plyFromRoot, move);
                moveOrderer.addHistoryBonus(move, depth);
                break;
            }
        }

        int flag;
        if (bestEvalInThisPosition <= oldAlpha) flag = TranspositionTable.UPPER; // fail‑low
        else if (bestEvalInThisPosition >= beta) flag = TranspositionTable.LOWER; // fail‑high
        else flag = TranspositionTable.EXACT; // exact

        transpositionTable.store(key, depth, bestEvalInThisPosition, flag, bestMoveInThisPosition);

        if (plyFromRoot == 0) {
            bestMove = bestMoveInThisPosition;
            bestEval = bestEvalInThisPosition;
        }

        return bestEvalInThisPosition;
    }

    // ──────────────────  fast evaluation  ──────────────────
    private int evaluate(GameState b) {
        Piece.Color stm = b.getSideToMove();
        Piece.Color opp = (stm == Piece.Color.RED) ? Piece.Color.BLUE : Piece.Color.RED;

        int n = BOARD_SIZE;
        int dSTM = connectionDistanceFast(b, stm, n);
        int dOPP = connectionDistanceFast(b, opp, n);

        int distTerm = W_DIST * (dOPP - dSTM);
        int bridgeTerm = W_BRID * (countBridges(b, stm, n) - countBridges(b, opp, n));
        int centTerm = W_CENT * (centralityScore(b, stm, n) - centralityScore(b, opp, n));
        return distTerm + bridgeTerm + centTerm;
    }

    // ---------- 0‑1 BFS connection distance ----------
    private int[] distBuf = new int[0];                  // reused scratch space
    private int connectionDistanceFast(GameState b, Piece.Color pl, int n) {
        int size = n * n;
        if (distBuf.length < size) distBuf = new int[size];
        Arrays.fill(distBuf, 0, size, INF);
        ArrayDeque<Integer> dq = new ArrayDeque<>(BOARD_SIZE * 2);

        // seed start edge
        for (int idx = 0; idx < size; idx++) {
            if (isStartEdge(idx, n, pl)) {
                Piece pc = b.getPiece(idx);
                if (pc != null && pc.getColor() != pl) continue;   // blocked
                int w = (pc == null) ? 1 : 0;
                distBuf[idx] = w;
                if (w == 0) dq.addFirst(idx); else dq.addLast(idx);
            }
        }
        while (!dq.isEmpty()) {
            int v = dq.pollFirst();
            int d = distBuf[v];
            if (isGoalEdge(v, n, pl)) return d;
            for (int nb : neighbours(v, n)) {
                Piece pc = b.getPiece(nb);
                if (pc != null && pc.getColor() != pl) continue;   // enemy blocks
                int w = (pc == null) ? 1 : 0;
                int nd = d + w;
                if (nd < distBuf[nb]) {
                    distBuf[nb] = nd;
                    if (w == 0) dq.addFirst(nb); else dq.addLast(nb);
                }
            }
        }
        return INF; // disconnected
    }

    private boolean isStartEdge(int idx, int n, Piece.Color pl) {
        return (pl == Piece.Color.RED) ? (idx % n == 0) : (idx / n == 0);
    }
    private boolean isGoalEdge(int idx, int n, Piece.Color pl) {
        return (pl == Piece.Color.RED) ? (idx % n == n - 1) : (idx / n == n - 1);
    }

    // ---------- neighbour list cache ----------
    private static final Map<Integer,int[][]> NB_CACHE = new HashMap<>();
    private static int[][] neighbourTable(int n) {
        return NB_CACHE.computeIfAbsent(n, size -> {
            int sz = size * size;
            int[][] tbl = new int[sz][];
            int[] dr = {-1,-1,0,0,1,1};
            int[] dc = {0,1,-1,1,-1,0};
            for (int idx = 0; idx < sz; idx++) {
                int r = idx / size, c = idx % size;
                int[] nb = new int[6]; int cnt = 0;
                for (int k = 0; k < 6; k++) {
                    int nr = r + dr[k], nc = c + dc[k];
                    if (nr>=0&&nr<size&&nc>=0&&nc<size) nb[cnt++] = nr*size+nc;
                }
                tbl[idx] = Arrays.copyOf(nb, cnt);
            }
            return tbl;
        });
    }
    private int[] neighbours(int idx, int n) { return neighbourTable(n)[idx]; }

    // ---------- bridge counting (lightweight) ----------
    private static final int[][] BR_OFF = {{-1,2},{1,1},{2,-1}};
    private int countBridges(GameState b, Piece.Color pl, int n) {
        int cnt = 0; int size = n*n;
        for (int idx = 0; idx < size; idx++) {
            Piece p = b.getPiece(idx);
            if (p == null || p.getColor()!=pl) continue;
            int r = idx / n, c = idx % n;
            for (int[] o: BR_OFF) {
                int nr = r+o[0], nc = c+o[1];
                if (nr<0||nr>=n||nc<0||nc>=n) continue;
                int tgt = nr*n+nc; if (b.getPiece(tgt) == null || b.getPiece(tgt).getColor() != pl) continue;
                if (o[0] == 2) {
                    nr = r = r+1;
                }
                if (o[1] == 2) {
                    nc = c = c+1;
                }
                int mid1 = r*n + nc;
                int mid2 = nr*n + c;
                if (b.getPiece(mid1)==null && b.getPiece(mid2)==null) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    // ---------- centrality (pre‑computed table) ----------
    private static final Map<Integer,int[]> CENT_CACHE = new HashMap<>();
    private int centralityScore(GameState b, Piece.Color pl, int n) {
        int[] table = CENT_CACHE.computeIfAbsent(n, size->{
            int sz=size*size; int[] t=new int[sz];
            double ctr=(size-1)/2.0;
            for(int i=0;i<sz;i++){
                int r=i/size,c=i%size;
                t[i]=(int)(10 - (Math.abs(r-ctr)+Math.abs(c-ctr))); // higher = better
            }
            return t;
        });
        int sum=0, sz=n*n;
        for(int idx=0;idx<sz;idx++){
            Piece p=b.getPiece(idx);
            if(p!=null && p.getColor()==pl) sum+=table[idx];
        }
        return sum;
    }

    private boolean isWin(int eval) {
        return Math.abs(eval) > 100000;
    }
}
