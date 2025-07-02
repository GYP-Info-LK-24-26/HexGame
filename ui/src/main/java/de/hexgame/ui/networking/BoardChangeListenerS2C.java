package de.hexgame.ui.networking;

import de.hexgame.logic.Piece;
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
        buf.writeEnum(UIGameBoard.get().getGameState().getPiece(move).getColor());
        HexServer.sendToEveryone("boardChange",buf);
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
                Piece.Color color = packetByteBuf.readEnum(Piece.Color.class);
                Position pos = new Position(index);
                UIGameBoard.get().getGameState().setPiece(pos,new Piece(color));
                UIGameBoard.get().onPlayerMove(pos);
                break;
            case CLEAR_BOARD:
                UIGameBoard.get().endGame();
                break;
        }
    }
}
