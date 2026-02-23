package de.hexgame.uifx.networking.packets;

import de.hexgame.logic.Player;
import de.hexgame.logic.PlayerWinListener;
import de.hexgame.uifx.networking.HexByteBuf;
import de.hexgame.uifx.networking.HexServer;

public class GameEndHandler implements PlayerWinListener {
    private final HexServer server;

    public GameEndHandler(HexServer server) {
        this.server = server;
    }

    @Override
    public void onPlayerWin(Player player) {
        HexByteBuf buf = HexByteBuf.create();
        buf.writeString(player.getName());
        server.sendToAll("end_game", buf.toByteArray());
    }
}
