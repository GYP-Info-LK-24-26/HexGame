package de.hexgame.ui.networking;

import de.igelstudios.igelengine.common.networking.ErrorHandler;
import de.igelstudios.igelengine.common.networking.client.ClientNet;
import de.igelstudios.igelengine.common.networking.server.ConnectionListener;
import de.igelstudios.igelengine.common.networking.server.Server;
import de.igelstudios.igelengine.common.util.PlayerFactory;

import java.util.*;

public class HexServer implements ConnectionListener {
    Server server;
    Map<UUID, ClientNet> players;
    ArrayList<RemotePlayer> player;

    public static void register(){
        Server.registerClient2ServerHandler("makeMove", MakeMoveRecieverC2S::recieve);
        Server.registerClient2ServerHandler("connect",PlayerConnectPacketC2S::recieve);
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
        player = new ArrayList<>();

        //init code for other classes
        PlayerFactory.setPlayerClass(RemotePlayer.class,true);
        register();
    }

    @Override
    public void playerConnect(ClientNet player) {
        this.player.add((RemotePlayer) player);
    }

    @Override
    public void playerDisConnect(ClientNet player) {
        this.player.remove((RemotePlayer) player);
    }

    public ArrayList<RemotePlayer> getPlayerList() {
        return player;
    }
}
