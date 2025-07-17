package de.hexgame.ui.gui;

import de.igelstudios.igelengine.client.ClientConfig;
import de.igelstudios.igelengine.client.gui.*;
import de.igelstudios.igelengine.client.lang.Text;
import de.igelstudios.igelengine.common.io.EngineSettings;
import org.joml.Vector2f;

public class SettingsGUI extends GUI {

    public SettingsGUI() {
        GUIManager.setGUI(this);

        TextField lang = new TextField(new Vector2f(35,35),new Vector2f(5,1), (String) ClientConfig.getConfig().get("lang")).addBackground(0x0000FFFF,0x00FFFFFF).addLabel(Text.translatable("lang"));
        addTextField(lang);


        Button save = new Button(new Vector2f(35,33),Text.translatable("save").setColor(0,1,0));
        addButton(save);

        save.addListener(new ButtonClickEvent() {
            @Override
            public void clicked(MouseButton button) {
                if(button != MouseButton.LMB)return;
                ClientConfig.getConfig().write("lang", lang.getContent().toLowerCase());

                render(Text.translatable("saved").setColor(0,1,0),35,31);
                Text.init((String) ClientConfig.getConfig().getOrDefault("lang", EngineSettings.parser("info.json").read().getDefaultLang()));
            }
        });
    }

}
