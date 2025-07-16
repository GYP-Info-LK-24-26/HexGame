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
        //Renderer.get().clear();
        GUIManager.setGUI(this);
        TextField hostField = new TextField(new Vector2f(35,35),new Vector2f(10,1)).addBackground(0x0000FFFF,0x00FFFFFF).addLabel(Text.translatable("host"));
        addTextField(hostField);

        TextField nameField = new TextField(new Vector2f(35,33),new Vector2f(10,1)).addBackground(0x0000FFFF,0x00FFFFFF).addLabel(Text.translatable("name"));
        addTextField(nameField);

        Button button = new  Button(new Vector2f(35,31),new Vector2f(5,1));
        addButton(button);
        render(Text.translatable("connect").setColor(0,1,0),35,31);

        button.addListener(button1 -> {
            if(button1 != MouseButton.LMB)return;
            new HexClient(hostField.getContent());
            PacketByteBuf buf = PacketByteBuf.create();
            buf.writeString(nameField.getContent());
            Client.send2Server("connect",buf);
        });
    }

    public void connectedSuccessfully(){
        Button button = new  Button(new Vector2f(35,29),new Vector2f(5,1));
        addButton(button);
        render(Text.translatable("connect_suc").setColor(0,1,0),35,29);
    }

    public void notConnected(){
        Button button = new  Button(new Vector2f(35,29),new Vector2f(5,1));
        addButton(button);
        render(Text.translatable("connect_un").setColor(0,1,0),35,29);
    }
}
