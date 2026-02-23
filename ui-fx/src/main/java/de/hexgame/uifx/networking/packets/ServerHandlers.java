package de.hexgame.uifx.networking.packets;

import de.hexgame.logic.Position;
import de.hexgame.uifx.networking.*;

public class ServerHandlers {

    public static void registerAll(PacketDispatcher dispatcher, HexServer server) {
        dispatcher.register("makeMove", (ctx, buf) -> handleMakeMove(ctx.channel(), buf, server));
        dispatcher.register("connect", (ctx, buf) -> handleConnect(ctx.channel(), buf, server));
    }

    private static void handleMakeMove(io.netty.channel.Channel channel, HexByteBuf buf, HexServer server) {
        RemotePlayer player = server.getPlayerByChannel(channel);
        if (player != null) {
            long time = buf.readLong();
            int index = buf.readInt();
            player.makeMove(new Position(index), time);
        }
    }

    private static void handleConnect(io.netty.channel.Channel channel, HexByteBuf buf, HexServer server) {
        String name = buf.readString();
        RemotePlayer player = server.getPlayerByChannel(channel);
        if (player != null) {
            player.setPlayerName(name);
            HexByteBuf resp = HexByteBuf.create();
            resp.writeEnum(HexServer.ConnectionState.CONNECTED);
            server.sendTo(player, "connected", resp.toByteArray());
        }
    }
}
