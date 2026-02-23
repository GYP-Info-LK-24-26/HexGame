package de.hexgame.uifx.networking.packets;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Player;
import de.hexgame.logic.PlayerMoveListener;
import de.hexgame.logic.Position;
import de.hexgame.uifx.networking.HexByteBuf;
import de.hexgame.uifx.networking.HexServer;

public class BoardChangeHandler implements PlayerMoveListener {
    private final GameState gameState;
    private final HexServer server;

    public BoardChangeHandler(GameState gameState, HexServer server) {
        this.gameState = gameState;
        this.server = server;
    }

    @Override
    public void onPlayerMove(Position move) {
        HexByteBuf buf = HexByteBuf.create();
        buf.writeEnum(ChangeState.ADD_HEX_COLOR);
        buf.writeInt(move.getIndex());
        buf.writeEnum(gameState.getPiece(move).getColor());
        server.sendToAll("boardChange", buf.toByteArray());
    }

    @Override
    public void onPlayerPreMove(Player player) {
    }

    public enum ChangeState {
        ADD_HEX_COLOR, CLEAR_BOARD
    }
}
