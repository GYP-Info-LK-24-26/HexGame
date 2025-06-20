package de.hexgame.ui;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;
import de.hexgame.logic.Util;

public class UIPlayer implements Player {
    //TODO add name setting in settings menu
    private final String playerName = "";
    @Override
    public String getName() {
        return playerName;
    }
    private boolean isMoving = false;
    private Move nextMove = null;
    private long maxMoveTime = 0L;
    public synchronized void makeMove(Move move) {
        if(!isMoving)return;
        nextMove = move;
        notify();
    }

    @Override
    public synchronized Move think(GameState gameState) {
        isMoving = true;
        try {
            if(maxMoveTime <= 0L)wait();
            else wait(maxMoveTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        isMoving = false;
        if(nextMove == null)return Util.generateRandomMove(gameState);
        Move ret = nextMove;
        nextMove = null;
        return ret;
    }
}
