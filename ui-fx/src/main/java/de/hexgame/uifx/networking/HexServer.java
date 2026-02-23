package de.hexgame.uifx.networking;

import de.hexgame.uifx.networking.packets.ServerHandlers;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HexServer {
    private static HexServer instance;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private Channel serverChannel;
    private final Map<Channel, RemotePlayer> playersByChannel = new ConcurrentHashMap<>();
    private final List<RemotePlayer> relevantPlayers = new ArrayList<>();
    private final PacketDispatcher dispatcher = new PacketDispatcher();

    public HexServer(int port) {
        if (instance != null) {
            throw new IllegalStateException("HexServer instance already exists. Call stop() first.");
        }
        instance = this;
        ServerHandlers.registerAll(dispatcher, this);

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        RemotePlayer player = new RemotePlayer(UUID.randomUUID());
                        player.setChannel(ch);
                        playersByChannel.put(ch, player);

                        ch.pipeline().addLast(new PacketDecoder(), new PacketEncoder(),
                                new ServerDispatcherAdapter(dispatcher, player));
                    }
                });

        try {
            serverChannel = b.bind(port).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public RemotePlayer getPlayerByChannel(Channel ch) {
        return playersByChannel.get(ch);
    }

    public void removePlayer(Channel ch) {
        playersByChannel.remove(ch);
    }

    public boolean isRelevantPlayer(RemotePlayer player) {
        return relevantPlayers.contains(player);
    }

    public void sendToAll(String type, byte[] payload) {
        for (RemotePlayer player : playersByChannel.values()) {
            player.getChannel().writeAndFlush(new HexPacket(type, payload));
        }
    }

    public void sendTo(RemotePlayer player, String type, byte[] payload) {
        player.getChannel().writeAndFlush(new HexPacket(type, payload));
    }

    public List<RemotePlayer> getPlayerList() {
        return new ArrayList<>(playersByChannel.values());
    }

    public void addRelevantPlayer(RemotePlayer player) {
        relevantPlayers.add(player);
    }

    public static HexServer getInstance() {
        return instance;
    }

    public static void stop() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }

    public static void forceStop() {
        stop();
    }

    public static void finish() {
        if (instance != null) {
            instance.relevantPlayers.forEach(RemotePlayer::end);
        }
    }

    private void shutdown() {
        if (serverChannel != null) serverChannel.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public enum ConnectionState {
        CONNECTED,
        DISCONNECTED
    }

    private static class ServerDispatcherAdapter extends SimpleChannelInboundHandler<HexPacket> {
        private final PacketDispatcher dispatcher;
        private final RemotePlayer player;

        ServerDispatcherAdapter(PacketDispatcher dispatcher, RemotePlayer player) {
            this.dispatcher = dispatcher;
            this.player = player;
        }

        @Override
        protected void messageReceived(ChannelHandlerContext ctx, HexPacket msg) {
            dispatcher.dispatch(ctx, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            HexServer srv = HexServer.getInstance();
            if (srv != null) {
                srv.removePlayer(ctx.channel());
                if (srv.isRelevantPlayer(player)) {
                    forceStop();
                    javafx.application.Platform.runLater(() -> {
                        de.hexgame.uifx.board.HexBoardController.get().endGame();
                        de.hexgame.uifx.gui.MainMenuPane pane = new de.hexgame.uifx.gui.MainMenuPane();
                        pane.showMessage(de.hexgame.uifx.TranslationManager.get().translate("player_disCon"));
                        de.hexgame.uifx.NavigationManager.get().navigateTo(pane);
                    });
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
