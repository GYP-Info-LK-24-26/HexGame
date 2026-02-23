package de.hexgame.uifx.networking;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class HexByteBuf {
    private final ByteBuf buf;

    public HexByteBuf(ByteBuf buf) {
        this.buf = buf;
    }

    public static HexByteBuf create() {
        return new HexByteBuf(Unpooled.buffer());
    }

    public static HexByteBuf wrap(ByteBuf buf) {
        return new HexByteBuf(buf);
    }

    public void writeInt(int value) {
        buf.writeInt(value);
    }

    public int readInt() {
        return buf.readInt();
    }

    public void writeLong(long value) {
        buf.writeLong(value);
    }

    public long readLong() {
        return buf.readLong();
    }

    public void writeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public String readString() {
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public <T extends Enum<T>> void writeEnum(T value) {
        buf.writeInt(value.ordinal());
    }

    public <T extends Enum<T>> T readEnum(Class<T> clazz) {
        int ordinal = buf.readInt();
        return clazz.getEnumConstants()[ordinal];
    }

    public ByteBuf getUnderlying() {
        return buf;
    }

    public byte[] toByteArray() {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        return bytes;
    }
}
