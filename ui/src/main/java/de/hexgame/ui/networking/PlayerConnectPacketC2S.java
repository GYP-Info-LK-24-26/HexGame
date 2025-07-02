package de.hexgame.ui.networking;

import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.ClientNet;
import de.igelstudios.igelengine.common.networking.server.Server;

public class PlayerConnectPacketC2S {
    public static void recieve(ClientNet clientNet, PacketByteBuf packetByteBuf) {
        String name = packetByteBuf.readString();
        ((RemotePlayer)clientNet).setPlayerName(name);
        PacketByteBuf send = PacketByteBuf.create();
        send.writeEnum(ConnectionStateS2C.State.CONNECTED);
        Server.send2Client(clientNet,"connected",send);
    }
}
