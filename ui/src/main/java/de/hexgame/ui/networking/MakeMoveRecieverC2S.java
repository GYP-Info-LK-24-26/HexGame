package de.hexgame.ui.networking;

import de.hexgame.logic.Position;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.ClientNet;

public class MakeMoveRecieverC2S {
    public static void recieve(ClientNet clientNet, PacketByteBuf packetByteBuf) {
        long time = packetByteBuf.readLong();
        int index =  packetByteBuf.readInt();

        ((RemotePlayer)clientNet).makeMove(new Position(index),time);
    }
}
