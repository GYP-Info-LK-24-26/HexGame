package de.hexgame.ui.gui;

import de.hexgame.ui.networking.HexClient;
import de.igelstudios.igelengine.client.graphics.Line;
import de.igelstudios.igelengine.client.graphics.Polygon;
import de.igelstudios.igelengine.client.graphics.Renderer;
import de.igelstudios.igelengine.client.gui.*;
import de.igelstudios.igelengine.client.lang.Text;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.Client;
import org.joml.Vector2f;

public class ConnectGUI extends GUI {

    public ConnectGUI() {
        GUIManager.setGUI(this);
        TextField hostField = new TextField(new Vector2f(35,35),new Vector2f(10,1));
        addTextField(hostField);
        Polygon polygon = new Polygon(new Vector2f(35,35),new Vector2f(45,35),new Vector2f(45,36),new Vector2f(35,36)).setRGBA(0,0,1,1);
        render(polygon);

        TextField nameField = new TextField(new Vector2f(35,33),new Vector2f(10,1));
        addTextField(nameField);
        Polygon background = new Polygon(new Vector2f(35,33),new Vector2f(45,33),new Vector2f(45,34),new Vector2f(35,34)).setRGBA(0,0,1,1);
        render(background);

        Button button = new  Button(new Vector2f(35,33),new Vector2f(5,1));
        addButton(button);
        render(Text.translatable("connect").setColor(0,1,0),35,33);

        button.addListener(button1 -> {
            if(button1 != MouseButton.LMB)return;
            new HexClient(hostField.getContent());
            PacketByteBuf buf = PacketByteBuf.create();
            buf.writeString(nameField.getContent());
            Client.send2Server("connect",buf);
        });
    }
}
