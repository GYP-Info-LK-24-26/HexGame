package de.hexgame.logic;


/**
 * This constitutes a hook that when registered in the corresponding {@link GameState} gets notified once a player made a move
 */
public interface PlayerMoveListener {
    void onPlayerMove(Position move);
}
