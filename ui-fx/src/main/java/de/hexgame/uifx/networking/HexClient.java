package de.hexgame.uifx.networking;

import de.hexgame.logic.Position;
import de.hexgame.uifx.NavigationManager;
import de.hexgame.uifx.TranslationManager;
import de.hexgame.uifx.board.HexBoardController;
import de.hexgame.uifx.gui.MainMenuPane;
import de.hexgame.uifx.networking.packets.ClientHandlers;
import de.hexgame.uifx.networking.packets.ClientNetworkCallback;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import javafx.application.Platform;
import lombok.Getter;

public class HexClient {
    public static final int DEFAULT_PORT = 25567;
    @Getter
    private static HexClient instance;
    private final EventLoopGroup group = new NioEventLoopGroup();
    private Channel channel;
    @Getter
    private boolean connected = false;
    private boolean stopped = false;
    private final PacketDispatcher dispatcher = new PacketDispatcher();

    public HexClient(String host, ClientNetworkCallback callback) {
        if (instance != null) {
            throw new IllegalStateException("HexClient instance already exists. Call stop() first.");
        }
        instance = this;
        int port = DEFAULT_PORT;
        String hostAddr = host;
        if (host.contains(":")) {
            String[] parts = host.split(":");
            hostAddr = parts[0];
            port = Integer.parseInt(parts[1]);
        }

        ClientHandlers.registerAll(dispatcher, callback);

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new PacketDecoder(), new PacketEncoder(), new ClientHandler());
                    }
                });

        try {
            ChannelFuture cf = b.connect(hostAddr, port).sync();
            channel = cf.channel();
            connected = true;
        } catch (Exception e) {
            callback.onConnectionState(false);
            group.shutdownGracefully();
        }
    }

    public void sendConnect(String playerName) {
        HexByteBuf buf = HexByteBuf.create();
        buf.writeString(playerName);
        send("connect", buf.toByteArray());
    }

    void send(String type, byte[] payload) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new HexPacket(type, payload));
        }
    }

    public void sendMakeMove(Position pos) {
        HexByteBuf buf = HexByteBuf.create();
        buf.writeLong(System.currentTimeMillis());
        buf.writeInt(pos.getIndex());
        send("makeMove", buf.toByteArray());
    }

    public static void stop() {
        if (instance != null) {
            instance.stopped = true;
            instance.connected = false;
            instance.shutdown();
            instance = null;
            HexBoardController.get().setRemote(false);
        }
    }

    public static void forceStop() {
        stop();
    }

    private void shutdown() {
        if (channel != null) channel.close();
        group.shutdownGracefully();
    }

    private class ClientHandler extends SimpleChannelInboundHandler<HexPacket> {
        @Override
        protected void messageReceived(ChannelHandlerContext ctx, HexPacket msg) {
            dispatcher.dispatch(ctx, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            Platform.runLater(() -> {
                HexBoardController.get().endGame();
                MainMenuPane pane = new MainMenuPane();
                if (!stopped) pane.showMessage(TranslationManager.get().translate("con_interrupted"));
                NavigationManager.get().navigateTo(pane);
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
