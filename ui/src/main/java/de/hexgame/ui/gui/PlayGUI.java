package de.hexgame.ui.gui;

import de.hexgame.logic.Player;
import de.igelstudios.igelengine.client.gui.GUI;
import de.igelstudios.igelengine.client.gui.GUIManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayGUI extends GUI {
    private List<Class<? extends Player>> playerList;
    public PlayGUI() {
        GUIManager.setGUI(this);
        try(InputStream stream = this.getClass().getClassLoader().getResourceAsStream("playerClasses.txt")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));
            List<String> playerNames = reader.lines().toList();
            playerList = new ArrayList<>();
            for (String playerName : playerNames) {
                 playerList.add((Class<? extends Player>) Class.forName(playerName));

            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
