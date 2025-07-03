package de.hexgame.ui.gui;

import de.hexgame.ui.networking.HexServer;
import de.igelstudios.igelengine.client.graphics.Polygon;
import de.igelstudios.igelengine.client.graphics.Renderer;
import de.igelstudios.igelengine.client.gui.*;
import de.igelstudios.igelengine.client.lang.Text;
import de.igelstudios.igelengine.common.networking.client.Client;
import org.joml.Vector2f;

public class HostGUI extends GUI {

    public HostGUI() {
        GUIManager.setGUI(this);

        TextField hostField = new TextField(new Vector2f(35,35),new Vector2f(5,1));
        addTextField(hostField);
        Polygon polygon = new Polygon(new Vector2f(35,35),new Vector2f(40,35),new Vector2f(40,36),new Vector2f(35,36)).setRGBA(0,0,1,1);
        render(polygon);

        Button button = new  Button(new Vector2f(35,33),new Vector2f(5,1));
        addButton(button);
        render(Text.translatable("start").setColor(0,1,0),35,33);


        button.addListener(button1 -> {
            if(button1 != MouseButton.LMB)return;
            int port;
            if(hostField.getLength() == 0)port = Client.DEFAULT_PORT;
            else port = Integer.parseInt(hostField.getContent());
            HexServer server = new HexServer(port);
            new PlayGUI(true,server);
        });
    }
}
