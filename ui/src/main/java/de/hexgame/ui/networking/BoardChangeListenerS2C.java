package de.hexgame.ui.networking;

import de.hexgame.logic.Piece;
import de.hexgame.logic.Position;
import de.hexgame.ui.UIGameBoard;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.Client;

public class BoardChangeListenerS2C {
    public enum ChangeState{
        ADD_HEX_COLOR,
        CLEAR_BOARD
    }
    public static void recieve(Client client, PacketByteBuf packetByteBuf) {
        ChangeState state = packetByteBuf.readEnum(ChangeState.class);
        switch (state) {
            case ADD_HEX_COLOR:
                int index = packetByteBuf.readInt();
                Piece.Color color = packetByteBuf.readEnum(Piece.Color.class);
                UIGameBoard.get().getGameState().setPiece(new Position(index),new Piece(color));
                break;
            case CLEAR_BOARD:
                    UIGameBoard.get().endGame();
                    break;
        }
    }
}
