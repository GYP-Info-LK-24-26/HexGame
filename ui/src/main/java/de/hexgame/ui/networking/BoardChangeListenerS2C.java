package de.hexgame.ui.networking;

import de.hexgame.logic.Player;
import de.hexgame.logic.PlayerMoveListener;
import de.hexgame.logic.Position;
import de.hexgame.ui.UIGameBoard;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.Client;

public class BoardChangeListenerS2C implements PlayerMoveListener{

    @Override
    public void onPlayerMove(Position move) {
        PacketByteBuf buf = PacketByteBuf.create();
        buf.writeEnum(ChangeState.ADD_HEX_COLOR);
        buf.writeInt(move.getIndex());
        buf.writeInt(UIGameBoard.get().getGameState().getPiece(move));
        HexServer.sendToEveryone("boardChange",buf);
    }

    @Override
    public void onPlayerPreMove(Player player) {

    }

    public enum ChangeState{
        ADD_HEX_COLOR,
        CLEAR_BOARD
    }
    public static void recieve(Client client, PacketByteBuf packetByteBuf) {
        ChangeState state = packetByteBuf.readEnum(ChangeState.class);
        switch (state) {
            case ADD_HEX_COLOR:
                int index = packetByteBuf.readInt();
                int color = packetByteBuf.readInt();
                Position pos = new Position(index);
                UIGameBoard.get().getGameState().setPiece(pos, color);
                UIGameBoard.get().onPlayerMove(pos);
                break;
            case CLEAR_BOARD:
                UIGameBoard.get().endGame();
                break;
        }
    }
}
