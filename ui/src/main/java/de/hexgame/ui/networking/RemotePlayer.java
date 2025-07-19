package de.hexgame.ui.networking;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;
import de.hexgame.logic.Position;
import de.igelstudios.igelengine.common.networking.client.ClientNet;

import java.util.UUID;

public class RemotePlayer implements Player, ClientNet {
    private long moveStart;
    private boolean moving = false;
    private Position move;
    private UUID uuid;
    private String playerName;
    private GameState gameState;

    @Override
    public String getName() {
        return "";
    }

    public RemotePlayer(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public synchronized Move think(GameState gameState) {
        moveStart = System.currentTimeMillis();
        moving = true;
        this.gameState = gameState;
        try {
            wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        moving = false;
        return new Move(move);
    }

    public synchronized void makeMove(Position pos,long moveTime){
        if(moveTime < moveStart)return;
        if(!pos.isValid())return;
        if(!gameState.isLegalMove(new Move(pos)))return;
        if(!moving)return;
        move = pos;
        notify();
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public synchronized void end(){
        move = new Position(-1,-1);
        notify();
    }
}
