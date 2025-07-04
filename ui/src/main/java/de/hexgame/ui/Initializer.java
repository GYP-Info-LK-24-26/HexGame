package de.hexgame.ui;

import de.hexgame.logic.Game;
import de.hexgame.logic.RandomPlayer;
import de.hexgame.ui.gui.MainGUI;
import de.igelstudios.igelengine.client.graphics.Renderer;
import de.igelstudios.igelengine.client.gui.GUIManager;
import de.igelstudios.igelengine.client.keys.*;
import de.igelstudios.igelengine.common.startup.EngineInitializer;
import de.igelstudios.igelengine.common.startup.KeyInitializer;
import org.lwjgl.glfw.GLFW;

public class Initializer implements EngineInitializer,KeyListener {
    @Override
    public void registerKeys(KeyInitializer keyInitializer) {
        keyInitializer.add(GLFW.GLFW_KEY_ESCAPE,"esc");
        keyInitializer.add((MouseClickListener) UIGameBoard.get());
        keyInitializer.add((KeyListener) this);

    }

    //this is the main function for the UI part
    @Override
    public void onInitialize() {
        new MainGUI();
        HIDInput.activateListener(this);

        /*UIGameBoard.get().startRendering();
        //here one may add two opposing Players for startup,note that when setting two random players a timeout should be introduced so that the players do not move faster than the human can see
        UIPlayer local = new UIPlayer();
        UIGameBoard.setCurrentUIPlayer(local);
        Game game = new Game(local,new RandomPlayer());
        game.getGameState().addPlayerMoveListener(UIGameBoard.get());
        UIGameBoard.setGameState(game.getGameState());
        game.asThread().start();*/
    }

    @Override
    public void onEnd() {

    }

    @KeyHandler("esc")
    public void onKeyPressed(boolean pressed) {
        if(!pressed)return;
        if(UIGameBoard.get().isRunning())UIGameBoard.get().forceEnd();
        if(!(GUIManager.getGui() instanceof MainGUI)) new MainGUI();
    }
}
