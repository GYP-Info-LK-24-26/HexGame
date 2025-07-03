package de.hexgame.ui.networking;

import de.hexgame.logic.Player;
import de.hexgame.logic.PlayerWinListener;
import de.hexgame.ui.gui.WinGUI;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.Client;
import de.igelstudios.igelengine.common.networking.server.Server;

public class GameEndS2C implements PlayerWinListener {
    public static void recive(Client client, PacketByteBuf packetByteBuf) {
        String name = packetByteBuf.readString();
        new WinGUI(name);
    }

    @Override
    public void onPlayerWin(Player player) {
        PacketByteBuf buf = PacketByteBuf.create();
        buf.writeString(player.getName());
        HexServer.sendToEveryone("end_game",buf);
    }
}
