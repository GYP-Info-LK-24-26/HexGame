package de.hexgame.ui.networking;

import de.hexgame.ui.UIGameBoard;
import de.hexgame.ui.gui.ConnectGUI;
import de.igelstudios.igelengine.client.gui.GUIManager;
import de.igelstudios.igelengine.common.networking.ErrorHandler;
import de.igelstudios.igelengine.common.networking.client.Client;
import de.igelstudios.igelengine.common.networking.client.ClientConnectListener;

public class HexClient implements ClientConnectListener {
    private Client client;
    private static HexClient instance;

    public static void register() {
        Client.registerServer2ClientHandler("boardChange",BoardChangeListenerS2C::recieve);
        Client.registerServer2ClientHandler("start",GameStartS2C::recieve);
        Client.registerServer2ClientHandler("connected",ConnectionStateS2C::recive);
        Client.registerServer2ClientHandler("end_game",GameEndS2C::recive);
    }

    public HexClient(String host){
        instance = this;
        client = new Client(host, new ErrorHandler() {
            @Override
            public void handle(Throwable cause) {
                cause.printStackTrace();
            }
        });
        client.start();

        register();

        Client.addConnectionListener(this);

        UIGameBoard.get().setRemote(true);
    }

    public static void stop(){
        if(instance != null){
            instance.client.stopClient();
            instance = null;
        }
    }

    @Override
    public void playerConnect() {

    }

    @Override
    public void playerDisConnect() {

    }

    @Override
    public void connectionFailed() {
        if(GUIManager.getGui() instanceof ConnectGUI){
            ((ConnectGUI) GUIManager.getGui()).notConnected();
        }
    }
}
