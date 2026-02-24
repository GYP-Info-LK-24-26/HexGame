package de.hexgame.uifx.board;

import de.hexgame.logic.*;

import static de.hexgame.logic.GameState.RED;

public class UIPlayer implements Player {
    private static final long MOVE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes
    private String playerName = "";
    private boolean isMoving = false;
    private Move nextMove = null;
    private GameState gameState;

    @Override
    public String getName() {
        return playerName;
    }

    public synchronized void makeMove(Move move) {
        if (!isMoving) return;
        if (!move.targetHexagon().isValid() || !gameState.isLegalMove(move)) return;
        nextMove = move;
        notify();
    }

    @Override
    public synchronized Move think(GameState gameState) {
        playerName = gameState.getSideToMove() == RED ? "RED" : "BLUE";
        isMoving = true;
        this.gameState = gameState;
        try {
            wait(MOVE_TIMEOUT_MS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        isMoving = false;
        if (nextMove == null) return Util.generateRandomMove(gameState);
        Move ret = nextMove;
        nextMove = null;
        return ret;
    }

    public synchronized void end() {
        nextMove = new Move(new Position(-1, -1));
        notify();
    }
}
