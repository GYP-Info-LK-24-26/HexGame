package de.hexgame.ui.networking;

import de.hexgame.ui.UIGameBoard;
import de.hexgame.ui.gui.MainGUI;
import de.igelstudios.ClientMain;
import de.igelstudios.igelengine.common.networking.ErrorHandler;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.ClientNet;
import de.igelstudios.igelengine.common.networking.server.ConnectionListener;
import de.igelstudios.igelengine.common.networking.server.Server;
import de.igelstudios.igelengine.common.util.PlayerFactory;

import java.util.*;

public class HexServer implements ConnectionListener {
    private Server server;
    private Map<UUID, ClientNet> players;
    private ArrayList<RemotePlayer> player;
    private static HexServer instance;
    private List<RemotePlayer> relevantPlayers;

    public static void register(){
        Server.registerClient2ServerHandler("makeMove", MakeMoveRecieverC2S::recieve);
        Server.registerClient2ServerHandler("connect",PlayerConnectPacketC2S::recieve);
    }

    public HexServer(int port) {
        instance = this;
        Server.addConnectionListener(this);
        players = new HashMap<>();
        server = new Server(port, players, new ErrorHandler() {
            @Override
            public void handle(Throwable cause) {
                cause.printStackTrace();
            }
        });
        server.start();
        player = new ArrayList<>();
        relevantPlayers = new ArrayList<>();

        ClientMain.getInstance().getEngine().addTickable(server);

        //init code for other classes
        PlayerFactory.setPlayerClass(RemotePlayer.class,true);
        register();
    }

    public static void forceStop() {
        stop();
        UIGameBoard.get().endGame();
    }

    @Override
    public void playerConnect(ClientNet player) {
        this.player.add((RemotePlayer) player);
    }

    @Override
    public void playerDisConnect(ClientNet player) {
        this.player.remove((RemotePlayer) player);

        if(relevantPlayers.contains(player)) {
            forceStop();
            new MainGUI().playerDisconnect();
        }
    }

    public ArrayList<RemotePlayer> getPlayerList() {
        return player;
    }

    public static void stop(){
        if(instance != null){
            instance.server.stopServer();
            instance = null;
        }
    }

    public static void sendToEveryone(String type, PacketByteBuf packetByteBuf){
        for (RemotePlayer remotePlayer : instance.player) {
            Server.send2Client(remotePlayer,type,packetByteBuf);
        }
    }

    public static void addRelevantPlayer(RemotePlayer remotePlayer){
        instance.relevantPlayers.add(remotePlayer);
    }
}
