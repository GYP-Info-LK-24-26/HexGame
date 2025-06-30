package de.hexgame.ui.gui;

import de.hexgame.logic.Player;
import de.hexgame.ui.UIGameBoard;
import de.igelstudios.igelengine.client.gui.*;
import de.igelstudios.igelengine.client.lang.Text;
import org.joml.Vector2f;

public class WinGUI extends GUI {

    public WinGUI(Player player) {
        GUIManager.setGUI(this);
        Text text;
        if(UIGameBoard.get().getLocalPlayers().size() == 1) text = player.getName().equals(UIGameBoard.get().getLocalPlayers().getFirst().getName())? Text.translatable("won"):Text.translatable("loss");
        else text = Text.literal(player.getName() + " ").append(Text.translatable("player_won"));
        render(text,60,40);

        Button btn = new Button(new Vector2f(60,38),new Vector2f(5,2));
        addButton(btn);
        render(Text.translatable("Main menu"),60,38);
        btn.addListener(new ButtonClickEvent() {
            @Override
            public void clicked(MouseButton button) {
                if(button == MouseButton.LMB){
                    UIGameBoard.get().endGame();
                    new MainGUI();
                }
            }
        });
    }
}
