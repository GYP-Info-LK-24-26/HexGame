package de.hexgame.ui.networking;

import de.igelstudios.igelengine.common.networking.ErrorHandler;
import de.igelstudios.igelengine.common.networking.client.ClientNet;
import de.igelstudios.igelengine.common.networking.server.ConnectionListener;
import de.igelstudios.igelengine.common.networking.server.Server;
import de.igelstudios.igelengine.common.util.PlayerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HexServer implements ConnectionListener {
    Server server;
    Map<UUID, ClientNet> players;

    public static void register(){
        Server.registerClient2ServerHandler("makeMove", MakeMoveRecieverC2S::recieve);
    }

    public HexServer(int port) {
        Server.addConnectionListener(this);
        players = new HashMap<>();
        server = new Server(port, players, new ErrorHandler() {
            @Override
            public void handle(Throwable cause) {
                cause.printStackTrace();
            }
        });

        //init code for other classes
        PlayerFactory.setPlayerClass(RemotePlayer.class,true);
        register();
    }

    @Override
    public void playerConnect(ClientNet player) {

    }

    @Override
    public void playerDisConnect(ClientNet player) {

    }
}
