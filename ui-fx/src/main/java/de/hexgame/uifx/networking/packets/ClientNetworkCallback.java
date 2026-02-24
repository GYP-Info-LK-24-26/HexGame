package de.hexgame.uifx.networking.packets;

import de.hexgame.logic.Position;

public interface ClientNetworkCallback {
    void onBoardChange(Position pos, int color);
    void onBoardClear();
    void onGameStart();
    void onConnectionState(boolean connected);
    void onGameEnd(String playerName);
}
