package de.hexgame.logic;

/**
 * This constitutes a hook that when registered in the corresponding {@link Game} gets notified once a player changes its chances what it thinks is its chance to win
 */
public interface PlayerWinListener {
    void onPlayerWin(Player player);
}
