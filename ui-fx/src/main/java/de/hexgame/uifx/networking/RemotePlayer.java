package de.hexgame.uifx.networking;

import de.hexgame.logic.*;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public class RemotePlayer implements Player {
    private static final long MOVE_TIMEOUT_MS = 5 * 60 * 1000L; // 5 minutes
    private final UUID uuid;
    @Setter
    private String playerName = "";
    @Setter
    @Getter
    private Channel channel;
    private long moveStart;
    private boolean moving = false;
    private Position move;
    private GameState gameState;

    public RemotePlayer(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getName() {
        return playerName;
    }

    @Override
    public synchronized Move think(GameState gameState) {
        moveStart = System.currentTimeMillis();
        moving = true;
        this.gameState = gameState;
        try {
            wait(MOVE_TIMEOUT_MS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        moving = false;
        if (move == null) {
            return new Move(new Position(-1, -1));
        }
        return new Move(move);
    }

    public synchronized void makeMove(Position pos, long moveTime) {
        if (moveTime < moveStart) return;
        if (!pos.isValid()) return;
        if (!gameState.isLegalMove(new Move(pos))) return;
        if (!moving) return;
        move = pos;
        notify();
    }

    public synchronized void end() {
        move = new Position(-1, -1);
        notify();
    }
}
