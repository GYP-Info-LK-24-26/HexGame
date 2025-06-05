package de.hexgame.ui;

import de.hexgame.logic.*;

import de.igelstudios.igelengine.client.graphics.Renderer;
import de.igelstudios.igelengine.client.graphics.texture.TexturePool;
import de.igelstudios.igelengine.client.keys.*;
import de.igelstudios.igelengine.common.scene.SceneObject;
import lombok.Getter;
import org.joml.Vector2f;

import static java.lang.Thread.sleep;

/**
 * this class is instanced so only one instance may exist during runtime which is available via {@link UIGameBoard#get()}
 */
//the size of the screen is 80 * 45,the bottom row is kept clear to avoid collision with on-screen objects and to make it look better
public class UIGameBoard implements PlayerMoveListener, MouseClickListener {
    private static UIGameBoard instance;
    private GameState gameState;
    //this is the time that has pass between turn to avoid graphical overloading
    private static final int MIN_TIME_PER_TURN = 100;
    //this keeps track of the last time a move was made so that the minimum time can be enforced
    private long last_time_run = 0;
    @Getter
    private UIPlayer localPlayer;
    private Vector2f uniformSize;
    @Getter
    private float leftOffset;
    @Getter
    private float scale;


    private UIGameBoard() {

    }

    //this loads every necessary texture and creates the background for the board
    //to minimize the numbers of sampler textures are used twice and rotated
    //this case
    public void startRendering(){
        HIDInput.activateListener(this);
        TexturePool.getID("red_hex.png");
        TexturePool.getID("blue_hex.png");
        float xScale = 80 / ((float) GameState.BOARD_SIZE + 1.5f) / 1.5f;
        float yScale = 45 / ((float) GameState.BOARD_SIZE + 1.5f) / 1.5f;
        scale = yScale;

        float right = ((float) (GameState.BOARD_SIZE - 1) / 2 + (GameState.BOARD_SIZE)) * yScale;
        float size = right;
        float remainder = (80 - size) / 2;
        leftOffset = remainder;

        uniformSize = new Vector2f((float) (1 * yScale), (float) (2 * yScale));

        SceneObject objHigh = new SceneObject().setTex(TexturePool.getID("left_top_hex.png")).setSize(uniformSize);
        Renderer.get().render(objHigh,remainder,GameState.BOARD_SIZE * yScale * 1.5f);


        SceneObject objLow = new SceneObject().setTex(TexturePool.getID("left_top_hex.png")).setSize(uniformSize).setRotation(2);
        Renderer.get().render(objLow, ((float) (GameState.BOARD_SIZE - 1) / 2 + (GameState.BOARD_SIZE - 1)) * yScale + remainder, 1.5f * yScale);

        SceneObject objLL = new SceneObject().setTex(TexturePool.getID("left_bot_hex.png")).setSize(uniformSize);
        Renderer.get().render(objLL, ((float) (GameState.BOARD_SIZE - 1) / 2) * yScale + remainder, 1.5f * yScale);

        SceneObject objRR = new SceneObject().setTex(TexturePool.getID("left_bot_hex.png")).setSize(uniformSize).setRotation(2);
        Renderer.get().render(objRR,(GameState.BOARD_SIZE - 1) * yScale + remainder,GameState.BOARD_SIZE * yScale * 1.5f);

        for (int i = 1; i < GameState.BOARD_SIZE - 1; i++) {
            SceneObject objL = new SceneObject().setTex(TexturePool.getID("left_hex.png")).setSize(uniformSize);
            Renderer.get().render(objL, ((float) (GameState.BOARD_SIZE - i - 1) / 2) * yScale + remainder,((i + 1) * 1.5f * yScale));

            SceneObject objR = new SceneObject().setTex(TexturePool.getID("left_hex.png")).setSize(uniformSize).setRotation(2);
            Renderer.get().render(objR, ((float) (GameState.BOARD_SIZE - i - 1) / 2 + (GameState.BOARD_SIZE - 1)) * yScale + remainder, ((float) ((i + 1) * 1.5 * yScale)));

            SceneObject objH = new SceneObject().setTex(TexturePool.getID("top_hex.png")).setSize(uniformSize);
            Renderer.get().render(objH,i * yScale + remainder,(GameState.BOARD_SIZE) * yScale * 1.5f);

            SceneObject objD = new SceneObject().setTex(TexturePool.getID("top_hex.png")).setSize(uniformSize).setRotation(2);
            Renderer.get().render(objD, ((float) (GameState.BOARD_SIZE - 1) / 2 + i) * yScale + remainder, 1.5f * yScale);
        }


        for (int i = 1; i < GameState.BOARD_SIZE - 1; i++) {
            for (int j = 1; j < GameState.BOARD_SIZE - 1; j++) {
                SceneObject obj = new SceneObject().setTex(TexturePool.getID("hex.png")).setSize(uniformSize);
                Renderer.get().render(obj, ((float) (GameState.BOARD_SIZE - j - 1) / 2 + i) * yScale + remainder, (float) ((j + 1) * 1.5 * yScale));
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
        SceneObject obj = new SceneObject().setTex(TexturePool.getID(gameState.getPiece(move).getColor() == Piece.Color.RED? "red_hex.png":"blue_hex.png")).setSize(uniformSize);
        //if(gameState.getPiece(move).getColor() == Piece.Color.RED){

            //Renderer.get().render(obj, (float) move.row() / 2 + move.column(), (float) (43 - (move.row() * 1.5)));
        //}else{
            //SceneObject obj = new SceneObject().setTex(TexturePool.getID("blue_hex.png")).setSize(uniformSize);
            Renderer.get().render(obj, ((float) (move.row()) / 2 + move.column()) * scale + leftOffset, (float) ((GameState.BOARD_SIZE - move.row()) * 1.5 * scale));
        //}
        last_time_run = System.currentTimeMillis();
    }

    @KeyHandler("LMB")
    public void lmb(boolean pressed,double x,double y){
        if(!pressed)return;
        Position pos = Util.convertToGameCords(x, y);
        Move move = new Move(pos);
        if(gameState.isLegalMove(move))localPlayer.makeMove(move);

    }
}
