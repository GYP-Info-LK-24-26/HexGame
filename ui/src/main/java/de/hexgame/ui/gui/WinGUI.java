package de.hexgame.ui.gui;

import de.hexgame.logic.Player;
import de.hexgame.ui.UIGameBoard;
import de.hexgame.ui.UIPlayer;
import de.igelstudios.igelengine.client.gui.*;
import de.igelstudios.igelengine.client.lang.Text;
import org.joml.Vector2f;

public class WinGUI extends GUI {

    public WinGUI(Player player) {
        GUIManager.setGUI(this);
        Text text = player.getName().equals(UIGameBoard.get().getLocalPlayer().getName())? Text.translatable("won"):Text.translatable("loss");

        render(text,60,40);

        Button btn = new Button(new Vector2f(60,38),new Vector2f(5,2));
        addButton(btn);
        render(Text.translatable("Main menu"),60,38);
        btn.addListener(new ButtonClickEvent() {
            @Override
            public void clicked(MouseButton button) {
                if(button == MouseButton.LMB){
                    UIGameBoard.get().transparent();
                    new MainGUI();
                }
            }
        });
    }
}
