package de.hexgame.ui;

import de.hexgame.logic.*;
import de.hexgame.logic.Util;
import de.igelstudios.ClientMain;

public class UIPlayer implements Player {
    //TODO add name setting in settings menu
    private String playerName = "";
    @Override
    public String getName() {
        return playerName;
    }
    private boolean isMoving = false;
    private Move nextMove = null;
    private long maxMoveTime = 0L;
    private GameState gameState;
    public synchronized void makeMove(Move move) {
        if(!isMoving)return;
        if(!move.targetHexagon().isValid() || !gameState.isLegalMove(move))return;
        nextMove = move;
        notify();
    }

    @Override
    public synchronized Move think(GameState gameState) {
        playerName = gameState.getSideToMove().toString();
        isMoving = true;
        this.gameState = gameState;
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

    public synchronized void end(){
        nextMove = new Move(new Position(-1,-1));
        notify();
    }
}
