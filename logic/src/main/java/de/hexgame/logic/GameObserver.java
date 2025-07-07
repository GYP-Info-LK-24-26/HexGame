package de.hexgame.logic;

/**
 * This interface does not contain any functions itself but rather combines all other types of listeners into one
 */
public interface GameObserver extends WinChanceChangeListener,PlayerWinListener,PlayerMoveListener{
}
