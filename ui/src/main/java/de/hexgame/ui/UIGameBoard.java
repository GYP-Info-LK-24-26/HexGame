package de.hexgame.ui;

import de.hexgame.logic.*;

import de.igelstudios.ClientMain;
import de.igelstudios.igelengine.client.graphics.Line;
import de.igelstudios.igelengine.client.graphics.Polygon;
import de.igelstudios.igelengine.client.graphics.Renderer;
import de.igelstudios.igelengine.client.graphics.texture.TexturePool;
import de.igelstudios.igelengine.client.keys.*;
import de.igelstudios.igelengine.common.scene.SceneObject;
import lombok.Getter;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * this class is instanced so only one instance may exist during runtime which is available via {@link UIGameBoard#get()}
 */
//the size of the screen is 80 * 45,the bottom row is kept clear to avoid collision with on-screen objects and to make it look better
public class UIGameBoard implements PlayerMoveListener, MouseClickListener {
    private static UIGameBoard instance;
    private GameState gameState;
    private List<Line> lineList;
    private List<Polygon> hexagonList;
    //this is the time that has pass between turn to avoid graphical overloading
    private static final int MIN_TIME_PER_TURN = 100;
    //this keeps track of the last time a move was made so that the minimum time can be enforced
    private long last_time_run = 0;
    private UIPlayer localPlayer;
    private Vector2f uniformSize;
    private float leftOffset;
    private float scale;
    private float length;
    private float topOffset;
    private float yScale;
    private boolean rendering;

    private UIGameBoard() {
        lineList = new ArrayList<>();
        hexagonList = new ArrayList<>();
    }

    public void resumeRendering(){
        lineList.forEach(line -> line.setRGBA(0,0,0,1));
        hexagonList.forEach(hex -> hex.setColor(0,0,0,0));
    }

    //this loads every necessary texture and creates the background for the board
    //to minimize the numbers of sampler textures are used twice and rotated
    //this case
    public void startRendering(){
        HIDInput.activateListener(this);
        if(rendering)resumeRendering();
        rendering = true;
        //TexturePool.getID("red_hex.png");
        //TexturePool.getID("blue_hex.png");
        /*float xScale = 80 / ((float) GameState.BOARD_SIZE + 1.5f) / 1.5f;
        float yScale = 45 / ((float) GameState.BOARD_SIZE + 1.5f) / 1.5f;
        scale = yScale;

        float right = ((float) (GameState.BOARD_SIZE - 1) / 2 + (GameState.BOARD_SIZE)) * yScale;
        float size = right;
        float remainder = (80 - size) / 2;
        leftOffset = remainder;

        uniformSize = new Vector2f((float) (1 * yScale), (float) (2 * yScale));*/
        length = (float) (45 / ((GameState.BOARD_SIZE + 1) * 2 * Math.sin(Math.PI * 2/3)));
        float deltaX = (float) (Math.cos(Math.toRadians(330)) * length);
        float size = deltaX * GameState.BOARD_SIZE * 3;
        leftOffset = (80 - size) / 2;
        scale = (float) 45 / ((GameState.BOARD_SIZE + 1));
        yScale = length * 1.5f;

        Line base = new Line(new Vector2f(leftOffset, 45 - length * 2),90,length,0.25f, Line.Type.RIGHT);
        lineList.add(base);
        Renderer.get().render(base);



        for (int i = 0; i < GameState.BOARD_SIZE; i++) {
            Line topLeft = base.cloneFromEnd(30, length, 0.25f, Line.Type.RIGHT);
            lineList.add(topLeft);
            Renderer.get().render(topLeft);
            Line topRight = topLeft.cloneFromEnd(330, length, 0.25f, Line.Type.RIGHT);
            lineList.add(topRight);
            Renderer.get().render(topRight);

            Line baseCPY = topRight.cloneFromEnd(-90,length,0.25f, Line.Type.CENTER);
            lineList.add(baseCPY);
            Renderer.get().render(baseCPY);

            Line botRight = baseCPY.cloneFromEnd(-150, length, 0.25f, Line.Type.CENTER);
            lineList.add(botRight);
            Renderer.get().render(botRight);
            Line botLeft = botRight.cloneFromEnd(150, length, 0.25f, Line.Type.CENTER);
            lineList.add(botLeft);
            Renderer.get().render(botLeft);

            Polygon p = Polygon.fromLines(topLeft,topRight,baseCPY,botRight,botLeft,base);
            hexagonList.add(p);

            base = baseCPY.mirrored();
        }

        int currentID = 8;
        for (int i = 0; i < GameState.BOARD_SIZE - 1; i++) {
            Line innerBase = null;
            for (int j = 0; j < GameState.BOARD_SIZE - 1; j++) {
                if(i >= 1 && j == 9) {
                    currentID++;
                }else if(i >= 2 && j == 0)currentID += 6;
                Line topLeft = lineList.get(currentID + 2);
                Line topRight = lineList.get(currentID + 1);
                Line right = topRight.cloneFromEnd(-90, length, 0.25f, Line.Type.CENTER);
                lineList.add(right);
                Renderer.get().render(right);

                Line botRight = right.cloneFromEnd(-150, length, 0.25f, Line.Type.CENTER);
                lineList.add(botRight);
                Renderer.get().render(botRight);
                Line botLeft = botRight.cloneFromEnd(150, length, 0.25f, Line.Type.CENTER);
                lineList.add(botLeft);
                Renderer.get().render(botLeft);

                if(innerBase == null) {
                    innerBase = botLeft.cloneFromEnd(90, length, 0.25f, Line.Type.CENTER);
                    lineList.add(innerBase);
                    Renderer.get().render(innerBase);
                }

                //if(i >= 1 && j == 9) {
                    //currentID--;
                //}

                Line l = lineList.get(currentID - ((i >= 1 && (j == 9 || j == 0))?3:2));
                if(i == 0)
                    l = j == 0?innerBase:innerBase.mirrored();
                Polygon p = Polygon.fromLines(right,botRight,botLeft,l,topLeft,topRight);
                hexagonList.add(p);
                innerBase = right;
                currentID += i == 0?5:3;
            }

            Line topLeft = lineList.get(currentID - (i == 0?5:3));
            Line topRight = topLeft.cloneFromEnd(330, length, 0.25f, Line.Type.RIGHT);
            lineList.add(topRight);
            Renderer.get().render(topRight);
            Line right = topRight.cloneFromEnd(-90, length, 0.25f, Line.Type.CENTER);
            lineList.add(right);
            Renderer.get().render(right);

            Line botRight = right.cloneFromEnd(-150, length, 0.25f, Line.Type.CENTER);
            lineList.add(botRight);
            Renderer.get().render(botRight);
            Line botLeft = botRight.cloneFromEnd(150, length, 0.25f, Line.Type.CENTER);
            lineList.add(botLeft);
            Renderer.get().render(botLeft);

            innerBase = botLeft.cloneFromEnd(90, length, 0.25f, Line.Type.CENTER);
            lineList.add(innerBase);
            Renderer.get().render(innerBase);

            Polygon p = Polygon.fromLines(topLeft,topRight,right,botRight,botLeft,innerBase);
            hexagonList.add(p);

            currentID += i == 0?2:-1;
        }

        //topOffset = lineList.get(lineList.size() - 4).getStart().y;
        topOffset = lineList.getLast().getStart().y;

        /*Line base = new Line(new Vector2f(12,2),new Vector2f(10,2),0.25F).setType(Line.Type.RIGHT);
        Renderer.get().render(base);
        base = base.cloneFromEnd(120,2,0.25f, Line.Type.RIGHT);
        Renderer.get().render(base);
        for (int i = 0; i < (GameState.BOARD_SIZE - 1); i++) {
            base = base.cloneFromEnd(60,2,0.25f,Line.Type.RIGHT);
            Renderer.get().render(base);
            Line par = base.cloneFromEnd(0,2,0.25f,Line.Type.CENTER);
            Renderer.get().render(par);
            base = base.cloneFromEnd(120,2,0.25f,Line.Type.RIGHT);
            Renderer.get().render(base);
        }*/
        //Renderer.get().render(new Line(new Vector2f(10,2),120,2,0.25F));
        //Renderer.get().render(new Line(new Vector2f(10,2),0,2,0.25F));

        /*SceneObject objHigh = new SceneObject().setTex(TexturePool.getID("left_top_hex.png")).setSize(uniformSize);
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
        }*/

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
        Polygon hex = hexagonList.get(move.getIndex());
        if(gameState.getPiece(move).getColor() == Piece.Color.RED)hex.setColor(1,0,0,1);
        else hex.setColor(0,0,1,1);
        Renderer.get().render(hex);
        //SceneObject obj = new SceneObject().setTex(TexturePool.getID(gameState.getPiece(move).getColor() == Piece.Color.RED? "red_hex.png":"blue_hex.png")).setSize(uniformSize);
        //if(gameState.getPiece(move).getColor() == Piece.Color.RED){

            //Renderer.get().render(obj, (float) move.row() / 2 + move.column(), (float) (43 - (move.row() * 1.5)));
        //}else{
            //SceneObject obj = new SceneObject().setTex(TexturePool.getID("blue_hex.png")).setSize(uniformSize);
            //Renderer.get().render(obj, ((float) (move.row()) / 2 + move.column()) * scale + leftOffset, (float) ((GameState.BOARD_SIZE - move.row()) * 1.5 * scale));
        //}
        last_time_run = System.currentTimeMillis();
    }

    @KeyHandler("LMB")
    public void lmb(boolean pressed,double x,double y){
        if(!pressed)return;
        System.out.println(ClientMain.getInstance().getEngine().getFPS());
        Position pos = Util.convertToGameCords(x, y);
        Move move = new Move(pos);
        if(gameState.isLegalMove(move))localPlayer.makeMove(move);

    }

    public UIPlayer getLocalPlayer() {
        return localPlayer;
    }

    public float getLeftOffset() {
        return leftOffset;
    }

    public float getScale() {
        return scale;
    }

    public float getLength() {
        return length;
    }

    public float getTopOffset() {
        return topOffset;
    }

    public float getyScale() {
        return yScale;
    }

    public void transparent(){
        hexagonList.forEach(hex -> hex.setA(0.5f));
        lineList.forEach(line -> line.setRGBA(0,0,0,0.5f));
    }
}
