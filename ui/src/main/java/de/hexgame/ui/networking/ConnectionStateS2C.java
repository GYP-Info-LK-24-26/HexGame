package de.hexgame.ui.networking;

import de.hexgame.ui.gui.ConnectGUI;
import de.igelstudios.igelengine.client.gui.GUIManager;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.Client;

public class ConnectionStateS2C {
    public enum State{
        CONNECTED,
        DISCONNECTED
    }

    public static void recive(Client client, PacketByteBuf packetByteBuf) {
        if(!(GUIManager.getGui() instanceof ConnectGUI))return;
        State state = packetByteBuf.readEnum(State.class);
        switch (state) {
            case CONNECTED:
                ((ConnectGUI) GUIManager.getGui()).connectedSuccessfully();
                break;
            case DISCONNECTED:
                ((ConnectGUI) GUIManager.getGui()).notConnected();
                break;
        }
    }
}
