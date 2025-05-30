package de.hexgame.logic;


//Hook for anything that needs an update when a move is made
public interface PlayerMoveListener {
    void onPlayerMove(Position move);
}
