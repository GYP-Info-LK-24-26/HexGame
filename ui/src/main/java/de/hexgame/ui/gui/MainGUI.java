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
        render(Text.translatable("play"),35,35);
        Button settingsBTN = new Button(new Vector2f(35,33),new Vector2f(5,1));
        addButton(settingsBTN);
        render(Text.translatable("settings"),35,33);
        Button quitBTN = new Button(new Vector2f(35,31),new Vector2f(5,1));
        addButton(quitBTN);
        render(Text.translatable("quit"),35,31);

        playBTN.addListener(e -> new PlayGUI());
        settingsBTN.addListener(e -> new SettingsGUI());
        quitBTN.addListener(e -> System.exit(0));
    }
}
