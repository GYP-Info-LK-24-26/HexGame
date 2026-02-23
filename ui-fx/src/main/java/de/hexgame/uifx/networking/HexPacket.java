package de.hexgame.uifx.networking;

public record HexPacket(String type, byte[] payload) {
}
