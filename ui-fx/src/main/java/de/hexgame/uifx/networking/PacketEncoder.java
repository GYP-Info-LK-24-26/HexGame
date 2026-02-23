package de.hexgame.uifx.networking;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

public class PacketEncoder extends MessageToByteEncoder<HexPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, HexPacket msg, ByteBuf out) {
        byte[] typeBytes = msg.type().getBytes(StandardCharsets.UTF_8);
        out.writeInt(typeBytes.length);
        out.writeBytes(typeBytes);
        out.writeInt(msg.payload().length);
        out.writeBytes(msg.payload());
    }
}
