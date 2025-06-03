package de.hexgame.logic;

/**
 * This interface is to be implemented by everything that can interact with the board
 *
 */
public interface Player {
    String getName();

    /**
     * this method is a hook that is called whenever the player has to move
     * @param gameState the current state of the game for the players orientation
     * @return the move the player choose to make
     */
    Move think(GameState gameState);

    void addPiece(Position position);
}
