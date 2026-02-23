package de.hexgame.uifx.networking;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class PacketDecoder extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int typeLen = in.readInt();
        byte[] typeBytes = new byte[typeLen];
        in.readBytes(typeBytes);
        String type = new String(typeBytes, StandardCharsets.UTF_8);

        int payloadLen = in.readInt();
        byte[] payload = new byte[payloadLen];
        in.readBytes(payload);

        out.add(new HexPacket(type, payload));
    }
}
