package de.hexgame.uifx.networking;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PacketDispatcher {
    private final Map<String, BiConsumer<ChannelHandlerContext, HexByteBuf>> handlers = new ConcurrentHashMap<>();

    public void register(String type, BiConsumer<ChannelHandlerContext, HexByteBuf> handler) {
        handlers.put(type, handler);
    }

    public void dispatch(ChannelHandlerContext ctx, HexPacket msg) {
        BiConsumer<ChannelHandlerContext, HexByteBuf> handler = handlers.get(msg.type());
        if (handler != null) {
            HexByteBuf buf = HexByteBuf.wrap(io.netty.buffer.Unpooled.wrappedBuffer(msg.payload()));
            handler.accept(ctx, buf);
        } else {
            System.err.println("No handler for packet type: " + msg.type());
        }
    }
}
