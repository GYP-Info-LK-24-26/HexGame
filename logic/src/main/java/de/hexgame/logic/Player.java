package de.hexgame.logic;

public interface Player {
    String getName();
    Move think(GameState gameState);
}
