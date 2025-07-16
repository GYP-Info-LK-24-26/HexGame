package de.hexgame.ui.gui;

import de.hexgame.logic.Game;
import de.hexgame.logic.Player;
import de.hexgame.ui.UIGameBoard;
import de.hexgame.ui.UIPlayer;
import de.hexgame.ui.networking.BoardChangeListenerS2C;
import de.hexgame.ui.networking.GameEndS2C;
import de.hexgame.ui.networking.HexServer;
import de.hexgame.ui.networking.RemotePlayer;
import de.igelstudios.igelengine.client.graphics.Line;
import de.igelstudios.igelengine.client.graphics.Renderer;
import de.igelstudios.igelengine.client.gui.*;
import de.igelstudios.igelengine.client.lang.Text;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import org.joml.Vector2f;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayGUI extends GUI {
    private static final int DIFFICULTIES = 6;
    private List<Class<? extends Player>> playerList;
    private List<Text> firstTexts;
    private List<Text> secondTexts;
    private int firstID = -1;
    private int secondID = -1;
    private Line difSelectorFirst;
    private Line difSelectorSecond;
    private int diffucultyFirst = 0;
    private int diffucultySecond = 0;

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

        difSelectorFirst = new Line(new Vector2f(29.75f,30 - playerList.size() * 4),0,0.5f,1, Line.Type.CENTER);
        render(difSelectorFirst);
        difSelectorSecond = new Line(new Vector2f(39.75f,30 - playerList.size() * 4),0,0.5f,1, Line.Type.CENTER);
        render(difSelectorSecond);

        Button startBtn = new Button(new Vector2f(36,34),Text.translatable("start").setColor(0,1,0));
        addButton(startBtn);
        startBtn.addListener(new ButtonClickEvent() {
            @Override
            public void clicked(MouseButton button) {
                try {
                    GUIManager.removeGui();
                    Player first = null;
                    Player second = null;
                    if(withRemote){
                        int i = 0;
                        if(firstID == 0) {
                            first = server.getPlayerList().get(i);
                            HexServer.addRelevantPlayer((RemotePlayer) first);
                            i++;
                        }else{
                            for (Constructor<?> declaredConstructor : playerList.get(firstID).getDeclaredConstructors()) {
                                if(declaredConstructor.getParameterCount() == 1 && declaredConstructor.getParameterTypes()[0] == int.class){
                                    first = playerList.get(firstID).getConstructor(int.class).newInstance(diffucultyFirst);
                                    break;
                                }
                            }
                            if(first == null) first = playerList.get(firstID).getConstructor().newInstance();
                        }
                        if(secondID == 0){
                            second = server.getPlayerList().get(i);
                            HexServer.addRelevantPlayer((RemotePlayer) second);
                        }else {
                            for (Constructor<?> declaredConstructor : playerList.get(secondID).getDeclaredConstructors()) {
                                if(declaredConstructor.getParameterCount() == 1 && declaredConstructor.getParameterTypes()[0] == int.class){
                                    second = playerList.get(secondID).getConstructor(int.class).newInstance(diffucultySecond);
                                    break;
                                }
                            }
                            if(second == null) second = playerList.get(secondID).getConstructor().newInstance();
                        }
                    }else{
                        for (Constructor<?> declaredConstructor : playerList.get(firstID).getDeclaredConstructors()) {
                            if(declaredConstructor.getParameterCount() == 1 && declaredConstructor.getParameterTypes()[0] == int.class){
                                first = playerList.get(firstID).getConstructor(int.class).newInstance(diffucultyFirst);
                                break;
                            }
                        }
                        if(first == null) first = playerList.get(firstID).getConstructor().newInstance();
                        for (Constructor<?> declaredConstructor : playerList.get(secondID).getDeclaredConstructors()) {
                            if(declaredConstructor.getParameterCount() == 1 && declaredConstructor.getParameterTypes()[0] == int.class){
                                second = playerList.get(secondID).getConstructor(int.class).newInstance(diffucultySecond);
                                break;
                            }
                        }
                        if(second == null) second = playerList.get(secondID).getConstructor().newInstance();
                    }
                    //if(first instanceof UIPlayer && second instanceof UIPlayer)return;
                    if(first instanceof UIPlayer)UIGameBoard.addPlayer((UIPlayer) first);
                    if(second instanceof UIPlayer)UIGameBoard.addPlayer((UIPlayer) second);
                    //Renderer.get().clear();
                    UIGameBoard.get().startRendering();
                    Game game = new Game(first, second);
                    UIGameBoard.get().init(game,first,second);
                    if(withRemote){
                        game.addPlayerMoveListener(new BoardChangeListenerS2C());
                        game.addPlayerWinListener(new GameEndS2C());
                    }
                    game.addPlayerWinListener(player -> new WinGUI(player.getName()));
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
            Button first = new Button(new Vector2f(30,30 - i * 4),firstText);
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
            Button second = new Button(new Vector2f(40,30 - i * 4),secondText);
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

        render(new Line(new Vector2f(30,30 - playerList.size() * 4),0,DIFFICULTIES - 1,0.25f, Line.Type.CENTER,0x96,0x4b,0,1));
        render(new Line(new Vector2f(40,30 - playerList.size() * 4),0,DIFFICULTIES - 1,0.25f, Line.Type.CENTER,0x96,0x4b,0,1));

        for (int i = 0; i < 2; i++) {
            final boolean side = i == 1;
            for (int j = 0; j < DIFFICULTIES; j++) {
                Button btn = new Button(new Vector2f(i * 10 + j + 29.5f,30 - playerList.size() * 4),new Vector2f(1,1));
                addButton(btn);
                int finalJ = j;
                btn.addListener(button -> {
                    if(button != MouseButton.LMB)return;
                    if(side){
                        diffucultySecond = finalJ;
                        difSelectorSecond.setStart(new Vector2f(39.75f + finalJ,30 - playerList.size() * 4));
                    }else{
                        diffucultyFirst = finalJ;
                        difSelectorFirst.setStart(new Vector2f(29.75f + finalJ,30 - playerList.size() * 4));
                    }
                });
            }
        }
    }
}
