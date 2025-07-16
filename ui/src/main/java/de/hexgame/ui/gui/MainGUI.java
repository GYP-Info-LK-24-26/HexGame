package de.hexgame.ui.gui;

import de.igelstudios.ClientMain;
import de.igelstudios.igelengine.client.gui.Button;
import de.igelstudios.igelengine.client.gui.GUI;
import de.igelstudios.igelengine.client.gui.GUIManager;
import de.igelstudios.igelengine.client.lang.Text;
import org.joml.Vector2f;

public class MainGUI extends GUI {

    public MainGUI() {
        super();
        GUIManager.setGUI(this);

        Button playBTN = new Button(new Vector2f(35,35),Text.translatable("play").setColor(0,1,0));
        addButton(playBTN);

        Button settingsBTN = new Button(new Vector2f(35,33),Text.translatable("settings").setColor(0,1,0));
        addButton(settingsBTN);

        Button connectBTN = new Button(new Vector2f(35,31),Text.translatable("connect").setColor(0,1,0));
        addButton(connectBTN);

        Button hostBTN = new Button(new Vector2f(35,29),Text.translatable("host").setColor(0,1,0));
        addButton(hostBTN);

        Button quitBTN = new Button(new Vector2f(35,27),Text.translatable("quit").setColor(0,1,0));
        addButton(quitBTN);

        playBTN.addListener(e ->
                new PlayGUI(false,null));
        settingsBTN.addListener(e -> new SettingsGUI());
        quitBTN.addListener(e -> ClientMain.getInstance().getEngine().stop());
        connectBTN.addListener(e -> new ConnectGUI());
        hostBTN.addListener(e -> new HostGUI());
    }

    public void disconnected() {
        render(Text.translatable("con_interrupted").setColor(0,1,0),35,37);
    }

    public void playerDisconnect() {
        render(Text.translatable("player_disCon").setColor(0,1,0),35,37);
    }
}
