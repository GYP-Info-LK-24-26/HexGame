package de.hexgame.ui;

import de.hexgame.logic.Game;
import de.hexgame.logic.RandomPlayer;
import de.hexgame.ui.gui.MainGUI;
import de.igelstudios.igelengine.client.graphics.Renderer;
import de.igelstudios.igelengine.client.keys.KeyListener;
import de.igelstudios.igelengine.client.keys.MouseClickListener;
import de.igelstudios.igelengine.client.keys.MouseMoveListener;
import de.igelstudios.igelengine.common.startup.EngineInitializer;
import de.igelstudios.igelengine.common.startup.KeyInitializer;

public class Initializer implements EngineInitializer {
    @Override
    public void registerKeys(KeyInitializer keyInitializer) {
        keyInitializer.add((MouseClickListener) UIGameBoard.get());
    }

    //this is the main function for the UI part
    @Override
    public void onInitialize() {
        new MainGUI();


        /*UIGameBoard.get().startRendering();
        //here one may add two opposing Players for startup,note that when setting two random players a timeout should be introduced so that the players do not move faster then the human can see
        UIPlayer local = new UIPlayer();
        UIGameBoard.setCurrentUIPlayer(local);
        Game game = new Game(local,new RandomPlayer());
        game.getGameState().addPlayerMoveListener(UIGameBoard.get());
        UIGameBoard.setGameState(game.getGameState());
        game.start();*/
    }

    @Override
    public void onEnd() {

    }
}
