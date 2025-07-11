package de.hexgame.ui.gui;

import de.igelstudios.igelengine.client.gui.Button;
import de.igelstudios.igelengine.client.gui.GUI;
import de.igelstudios.igelengine.client.gui.GUIManager;
import de.igelstudios.igelengine.client.lang.Text;
import org.joml.Vector2f;

public class MainGUI extends GUI {

    public MainGUI() {
        super();
        GUIManager.setGUI(this);

        Button playBTN = new Button(new Vector2f(35,35),new Vector2f(5,1));
        addButton(playBTN);
        render(Text.translatable("play").setColor(0,1,0),35,35);

        Button settingsBTN = new Button(new Vector2f(35,33),new Vector2f(5,1));
        addButton(settingsBTN);
        render(Text.translatable("settings").setColor(0,1,0),35,33);

        Button connectBTN = new Button(new Vector2f(35,31),new Vector2f(5,1));
        addButton(connectBTN);
        render(Text.translatable("connect").setColor(0,1,0),35,31);

        Button hostBTN = new Button(new Vector2f(35,29),new Vector2f(5,1));
        addButton(hostBTN);
        render(Text.translatable("host").setColor(0,1,0),35,29);

        Button quitBTN = new Button(new Vector2f(35,27),new Vector2f(5,1));
        addButton(quitBTN);
        render(Text.translatable("quit").setColor(0,1,0),35,27);

        playBTN.addListener(e ->
                new PlayGUI(false,null));
        settingsBTN.addListener(e -> new SettingsGUI());
        quitBTN.addListener(e -> System.exit(0));
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
