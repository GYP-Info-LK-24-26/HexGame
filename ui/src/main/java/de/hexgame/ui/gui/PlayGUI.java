package de.hexgame.ui.gui;

import de.hexgame.logic.Game;
import de.hexgame.logic.Player;
import de.hexgame.ui.UIGameBoard;
import de.hexgame.ui.UIPlayer;
import de.hexgame.ui.networking.BoardChangeListenerS2C;
import de.hexgame.ui.networking.GameEndS2C;
import de.hexgame.ui.networking.HexServer;
import de.hexgame.ui.networking.RemotePlayer;
import de.igelstudios.igelengine.client.graphics.Renderer;
import de.igelstudios.igelengine.client.gui.*;
import de.igelstudios.igelengine.client.lang.Text;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import org.joml.Vector2f;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayGUI extends GUI {
    private List<Class<? extends Player>> playerList;
    private List<Text> firstTexts;
    private List<Text> secondTexts;
    private int firstID = -1;
    private int secondID = -1;
    public PlayGUI(boolean withRemote,HexServer server) {
        GUIManager.setGUI(this);
        try(InputStream stream = this.getClass().getClassLoader().getResourceAsStream("playerClasses.txt")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));
            List<String> playerNames = reader.lines().toList();
            playerList = new ArrayList<>();
            if(withRemote) playerList.add(RemotePlayer.class);

            for (String playerName : playerNames) {
                try {
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(playerName);
                    }catch (ClassNotFoundException ignored){

                    }
                    if(clazz != null){
                        playerList.add((Class<? extends Player>) clazz);
                    }
                }catch (ClassCastException e){
                    throw new RuntimeException("Player Class has to inherit Player but " + playerName + " does not",e);
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        firstTexts = new ArrayList<>();
        secondTexts = new ArrayList<>();

        Button startBtn = new Button(new Vector2f(36,34),new Vector2f(1,4));
        addButton(startBtn);
        render(Text.translatable("start").setColor(0,1,0),36,34);
        startBtn.addListener(new ButtonClickEvent() {
            @Override
            public void clicked(MouseButton button) {
                try {
                    GUIManager.removeGui();
                    Player first;
                    Player second;
                    if(withRemote){
                        int i = 0;
                        if(firstID == 0) {
                            first = server.getPlayerList().get(i);
                            HexServer.addRelevantPlayer((RemotePlayer) first);
                            i++;
                        }else{
                            first = playerList.get(firstID).getConstructor().newInstance();
                        }
                        if(secondID == 0){
                            second = server.getPlayerList().get(i);
                            HexServer.addRelevantPlayer((RemotePlayer) second);
                        }else {
                            second = playerList.get(secondID).getConstructor().newInstance();
                        }
                    }else{
                        first = playerList.get(firstID).getConstructor().newInstance();
                        second = playerList.get(secondID).getConstructor().newInstance();
                    }
                    //if(first instanceof UIPlayer && second instanceof UIPlayer)return;
                    if(first instanceof UIPlayer)UIGameBoard.addPlayer((UIPlayer) first);
                    if(second instanceof UIPlayer)UIGameBoard.addPlayer((UIPlayer) second);
                    //Renderer.get().clear();
                    UIGameBoard.get().startRendering();
                    Game game = new Game(first, second);
                    game.getGameState().addPlayerMoveListener(UIGameBoard.get());
                    if(withRemote){
                        game.getGameState().addPlayerMoveListener(new BoardChangeListenerS2C());
                        game.addPlayerWinListener(new GameEndS2C());
                    }
                    game.addPlayerWinListener(player -> new WinGUI(player.getName()));
                    UIGameBoard.setGameState(game.getGameState());
                    game.asThread().start();

                    if(withRemote){
                        HexServer.sendToEveryone("start",PacketByteBuf.create());
                    }
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        for (int i = 0; i < playerList.size(); i++) {
            final int id = i;

            Text firstText = Text.translatable(playerList.get(i).getName()).setColor(0,1,0);
            render(firstText,30,30 - i * 4);
            Button first = new Button(new Vector2f(30,30 - i * 4),new Vector2f(5,1));
            addButton(first);
            first.addListener(new ButtonClickEvent() {
                @Override
                public void clicked(MouseButton button) {
                    if(firstID != -1)firstTexts.get(firstID).setColor(0,1,0);
                    firstID = id;
                    firstText.setColor(1,0,0);
                }
            });
            firstTexts.add(firstText);

            Text secondText = Text.translatable(playerList.get(i).getName()).setColor(0,1,0);
            render(secondText,40,30 - i * 4);
            Button second = new Button(new Vector2f(40,30 - i * 4),new Vector2f(5,1));
            addButton(second);
            second.addListener(new ButtonClickEvent() {
                @Override
                public void clicked(MouseButton button) {
                    if(secondID != -1) secondTexts.get(secondID).setColor(0,1,0);
                    secondID = id;
                    secondText.setColor(1,0,0);
                }
            });
            secondTexts.add(secondText);
        }
    }
}
