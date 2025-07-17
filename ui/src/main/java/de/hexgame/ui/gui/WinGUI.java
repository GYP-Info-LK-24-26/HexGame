package de.hexgame.ui.gui;

import de.hexgame.logic.Game;
import de.hexgame.logic.Player;
import de.hexgame.ui.UIGameBoard;
import de.hexgame.ui.networking.BoardChangeListenerS2C;
import de.hexgame.ui.networking.GameEndS2C;
import de.hexgame.ui.networking.HexClient;
import de.hexgame.ui.networking.HexServer;
import de.igelstudios.igelengine.client.gui.*;
import de.igelstudios.igelengine.client.lang.Text;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import org.joml.Vector2f;

public class WinGUI extends GUI {

    public WinGUI(String player) {
        GUIManager.setGUI(this);
        Text text;
        if(UIGameBoard.get().getLocalPlayers().size() == 1) text = player.equals(UIGameBoard.get().getLocalPlayers().getFirst().getName())? Text.translatable("won"):Text.translatable("loss");
        else text = Text.literal(player + " ").append(Text.translatable("player_won"));
        render(text,60,40);

        Button btn = new Button(new Vector2f(60,38),Text.translatable("main_menu"));
        addButton(btn);
        btn.addListener(new ButtonClickEvent() {
            @Override
            public void clicked(MouseButton button) {
                if(button == MouseButton.LMB){
                    UIGameBoard.get().endGame();
                    HexClient.stop();
                    HexServer.stop();
                    new MainGUI();
                }
            }
        });

        Button reRun = new Button(new Vector2f(60,36),Text.translatable("re_run"));
        addButton(reRun);
        reRun.addListener(new ButtonClickEvent() {
            @Override
            public void clicked(MouseButton button) {
                if(button != MouseButton.LMB)return;
                GUIManager.removeGui();
                UIGameBoard.get().endGame();
                UIGameBoard.get().startRendering();
                Game game = new Game(UIGameBoard.get().getPlayerA(), UIGameBoard.get().getPlayerB());
                UIGameBoard.get().init(game,UIGameBoard.get().getPlayerA(), UIGameBoard.get().getPlayerB());
                if(UIGameBoard.get().isRemote()){
                    game.addPlayerMoveListener(new BoardChangeListenerS2C());
                    game.addPlayerWinListener(new GameEndS2C());
                }
                game.addPlayerWinListener(player -> new WinGUI(player.getName()));
                game.asThread().start();

                if(UIGameBoard.get().isRemote()){
                    HexServer.sendToEveryone("start", PacketByteBuf.create());
                }
            }
        });
    }
}
