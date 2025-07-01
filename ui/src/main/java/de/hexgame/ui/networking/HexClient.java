package de.hexgame.ui.networking;

import de.hexgame.ui.UIGameBoard;
import de.igelstudios.igelengine.common.networking.ErrorHandler;
import de.igelstudios.igelengine.common.networking.client.Client;

public class HexClient {
    private Client client;

    public static void register() {
        Client.registerServer2ClientHandler("boardChange",BoardChangeListenerS2C::recieve);
    }

    public HexClient(String host){
        client = new Client(host, new ErrorHandler() {
            @Override
            public void handle(Throwable cause) {
                cause.printStackTrace();
            }
        });

        register();

        UIGameBoard.get().setRemote(true);
    }
}
