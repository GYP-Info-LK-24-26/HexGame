package de.hexgame.ui.networking;

import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.ClientNet;

public class PlayerConnectPacketC2S {
    public static void recieve(ClientNet clientNet, PacketByteBuf packetByteBuf) {
        String name = packetByteBuf.readString();
        ((RemotePlayer)clientNet).setPlayerName(name);
    }
}
