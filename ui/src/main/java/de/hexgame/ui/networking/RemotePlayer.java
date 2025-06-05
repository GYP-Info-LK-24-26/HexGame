package de.hexgame.ui.networking;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;

public class RemotePlayer implements Player {

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Move think(GameState gameState) {
        return null;
    }
}
