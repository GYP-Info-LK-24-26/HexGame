package de.hexgame.ui.networking;

import de.hexgame.logic.GameState;
import de.hexgame.ui.UIGameBoard;
import de.igelstudios.igelengine.client.gui.GUIManager;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.Client;

public class GameStartS2C {
    public static void recieve(Client client, PacketByteBuf packetByteBuf) {
        GUIManager.removeGui();
        UIGameBoard.get().startRendering();
        UIGameBoard.setGameState(new GameState());
    }
}
