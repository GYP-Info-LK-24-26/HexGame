package de.hexgame.ui;

import de.hexgame.logic.*;

import de.igelstudios.igelengine.client.graphics.Renderer;
import de.igelstudios.igelengine.client.graphics.texture.TexturePool;
import de.igelstudios.igelengine.client.keys.KeyHandler;
import de.igelstudios.igelengine.client.keys.KeyListener;
import de.igelstudios.igelengine.client.keys.MouseMoveListener;
import de.igelstudios.igelengine.common.scene.SceneObject;
import org.joml.Vector2f;

import static java.lang.Thread.sleep;

/**
 * this class is instanced eg only one instance may exist during one runtime available via {@link UIGameBoard#get()}
 */
public class UIGameBoard implements PlayerMoveListener, KeyListener, MouseMoveListener {
    private double mouseX, mouseY;
    private static UIGameBoard instance;
    private GameState gameState;
    //this is the time that has pass between turn to avoid graphical overloading
    private static final int MIN_TIME_PER_TURN = 100;
    //this keeps track of the last time a move was made so that the minimum time can be enforced
    private long last_time_run = 0;
    private UIPlayer localPlayer;


    private UIGameBoard() {

    }

    //this loads every necessary texture and creates the background for the board
    //to minimize the numbers of sampler textures are used twice and rotated
    //this case
    public void startRendering(){
        TexturePool.getID("red_hex.png");
        TexturePool.getID("blue_hex.png");

        SceneObject objHigh = new SceneObject().setTex(TexturePool.getID("left_top_hex.png")).setSize(new Vector2f(1,2));
        Renderer.get().render(objHigh,0,43);


        SceneObject objLow = new SceneObject().setTex(TexturePool.getID("left_top_hex.png")).setSize(new Vector2f(1,2)).setRotation(2);
        Renderer.get().render(objLow, (float) (GameState.BOARD_SIZE - 1) / 2 + (GameState.BOARD_SIZE - 1), (float) (43 - ((GameState.BOARD_SIZE - 1) * 1.5)));

        SceneObject objLL = new SceneObject().setTex(TexturePool.getID("left_bot_hex.png")).setSize(new Vector2f(1,2));
        Renderer.get().render(objLL, (float) (GameState.BOARD_SIZE - 1) / 2, (float) (43 - ((GameState.BOARD_SIZE - 1) * 1.5)));

        SceneObject objRR = new SceneObject().setTex(TexturePool.getID("left_bot_hex.png")).setSize(new Vector2f(1,2)).setRotation(2);
        Renderer.get().render(objRR,GameState.BOARD_SIZE - 1,43);

        for (int i = 1; i < GameState.BOARD_SIZE - 1; i++) {
            SceneObject objL = new SceneObject().setTex(TexturePool.getID("left_hex.png")).setSize(new Vector2f(1,2));
            Renderer.get().render(objL, (float) i / 2, (float) (43 - (i * 1.5)));

            SceneObject objR = new SceneObject().setTex(TexturePool.getID("left_hex.png")).setSize(new Vector2f(1,2)).setRotation(2);
            Renderer.get().render(objR, (float) i / 2 + (GameState.BOARD_SIZE - 1), (float) (43 - (i * 1.5)));

            SceneObject objH = new SceneObject().setTex(TexturePool.getID("top_hex.png")).setSize(new Vector2f(1,2));
            Renderer.get().render(objH,i,43);

            SceneObject objD = new SceneObject().setTex(TexturePool.getID("top_hex.png")).setSize(new Vector2f(1,2)).setRotation(2);
            Renderer.get().render(objD, (float) (GameState.BOARD_SIZE - 1) / 2 + i, (float) (43 - ((GameState.BOARD_SIZE - 1) * 1.5)));
        }


        for (int i = 1; i < GameState.BOARD_SIZE - 1; i++) {
            for (int j = 1; j < GameState.BOARD_SIZE - 1; j++) {
                SceneObject obj = new SceneObject().setTex(TexturePool.getID("hex.png")).setSize(new Vector2f(1,2));
                Renderer.get().render(obj, (float) j / 2 + i, (float) (43 - (j * 1.5)));
            }
        }

    }

    /**
     *
     * @return the only instance of {@link UIGameBoard}, this may never return null
     */
    public static UIGameBoard get() {
        if(instance == null)instance = new UIGameBoard();
        return instance;
    }

    /**
     * sets the {@link GameState} for the instance so that it may be visualised
     * @param gameState the gamestate to set
     */
    public static void setGameState(GameState gameState) {
        get().gameState = gameState;
    }

    /**
     * this sets the local {@link UIPlayer} for the instance e.g. the one that can make moves on this board
     * @param uiPlayer the UIPlayer
     */
    public static void setCurrentUIPlayer(UIPlayer uiPlayer) {
        get().localPlayer = uiPlayer;
    }

    //this function runs everytime a player move was made to render the change(add the played hexagon)
    @Override
    public void onPlayerMove(Position move) {
        //this ensures that the minimum duration between turns is kept and that no turn is made to early
        long deltaRun = System.currentTimeMillis() - last_time_run;
        if(deltaRun <= MIN_TIME_PER_TURN) {
            try {
                sleep(MIN_TIME_PER_TURN - deltaRun);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if(gameState.getPiece(move).getColor() == Piece.Color.RED){
            SceneObject obj = new SceneObject().setTex(TexturePool.getID("red_hex.png")).setSize(new Vector2f(1,2));
            Renderer.get().render(obj, (float) move.row() / 2 + move.column(), (float) (43 - (move.row() * 1.5)));
        }else{
            SceneObject obj = new SceneObject().setTex(TexturePool.getID("blue_hex.png")).setSize(new Vector2f(1,2));
            Renderer.get().render(obj, (float) move.row() / 2 + move.column(), (float) (43 - (move.row() * 1.5)));
        }
        last_time_run = System.currentTimeMillis();
    }

    @KeyHandler("LMB")
    public void lmb(boolean pressed){
        if(!pressed)return;
        Position pos = Util.convertToGameCords(mouseX, mouseY);
        Move move = new Move(pos);
        if(gameState.isLegalMove(move))localPlayer.makeMove(move);

    }

    @Override
    public void mouseMove(double x, double y) {
        mouseX = x;
        mouseY = y;
    }
}
