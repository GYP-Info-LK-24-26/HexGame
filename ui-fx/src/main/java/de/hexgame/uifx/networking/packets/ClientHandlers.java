package de.hexgame.uifx.networking.packets;

import de.hexgame.logic.Position;
import de.hexgame.uifx.networking.HexByteBuf;
import de.hexgame.uifx.networking.PacketDispatcher;
import javafx.application.Platform;

public class ClientHandlers {

    public static void registerAll(PacketDispatcher dispatcher, ClientNetworkCallback callback) {
        dispatcher.register("boardChange", (ctx, buf) -> handleBoardChange(buf, callback));
        dispatcher.register("start", (ctx, buf) -> handleGameStart(callback));
        dispatcher.register("connected", (ctx, buf) -> handleConnectionState(buf, callback));
        dispatcher.register("end_game", (ctx, buf) -> handleGameEnd(buf, callback));
    }

    private static void handleBoardChange(HexByteBuf buf, ClientNetworkCallback callback) {
        int stateOrd = buf.readInt();
        if (stateOrd == 0) { // ADD_HEX_COLOR
            int index = buf.readInt();
            int color = buf.readInt();
            Position pos = new Position(index);
            Platform.runLater(() -> callback.onBoardChange(pos, color));
        } else { // CLEAR_BOARD
            Platform.runLater(callback::onBoardClear);
        }
    }

    private static void handleGameStart(ClientNetworkCallback callback) {
        Platform.runLater(callback::onGameStart);
    }

    private static void handleConnectionState(HexByteBuf buf, ClientNetworkCallback callback) {
        int stateOrd = buf.readInt();
        boolean connected = stateOrd == 0;
        Platform.runLater(() -> callback.onConnectionState(connected));
    }

    private static void handleGameEnd(HexByteBuf buf, ClientNetworkCallback callback) {
        String name = buf.readString();
        Platform.runLater(() -> callback.onGameEnd(name));
    }
}
