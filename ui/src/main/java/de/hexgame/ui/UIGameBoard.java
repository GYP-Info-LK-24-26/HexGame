package de.hexgame.ui;

import de.hexgame.logic.*;

import de.hexgame.ui.networking.HexClient;
import de.hexgame.ui.networking.HexServer;
import de.igelstudios.igelengine.client.graphics.Line;
import de.igelstudios.igelengine.client.graphics.Polygon;
import de.igelstudios.igelengine.client.graphics.Renderer;
import de.igelstudios.igelengine.client.keys.*;
import de.igelstudios.igelengine.common.networking.PacketByteBuf;
import de.igelstudios.igelengine.common.networking.client.Client;
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
    private List<Polygon> cornerList;
    private List<Polygon> hexagonList;
    //this is the time that has pass between turn to avoid graphical overloading
    private static final int MIN_TIME_PER_TURN = 100;
    //this keeps track of the last time a move was made so that the minimum time can be enforced
    private long last_time_run = 0;
    private List<UIPlayer> playerList;
    private Vector2f uniformSize;
    private float leftOffset;
    private float scale;
    private float length;
    private float topOffset;
    private float yScale;
    private boolean rendering;
    private boolean isRemote = false;
    private boolean running = false;

    private UIGameBoard() {
        lineList = new ArrayList<>();
        cornerList = new ArrayList<>();
        hexagonList = new ArrayList<>();
        playerList = new ArrayList<>();
    }

    public void resumeRendering(){
        lineList.forEach(line -> line.setA(1));
        hexagonList.forEach(hex -> hex.setRGBA(1,1,1,1));
        cornerList.forEach(hex -> hex.setA(1));

        HIDInput.activateListener(this);
    }

    //this loads every necessary texture and creates the background for the board
    //to minimize the numbers of sampler textures are used twice and rotated
    //this case
    public void startRendering(){
        running = true;
        if(rendering){
            resumeRendering();
            return;
        }
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

        Line base = new Line(new Vector2f(leftOffset, 45 - length * 2),90,length,0.25f, Line.Type.CENTER).setRGBA(1,0,0,1);
        lineList.add(base);
        Renderer.get().render(base);

        for (int i = 0; i < GameState.BOARD_SIZE; i++) {
            Line topLeft = base.cloneFromEnd(30, length, 0.25f, Line.Type.CENTER).setRGBA(0,0,1,1);
            lineList.add(topLeft);
            Renderer.get().render(topLeft);
            Line topRight = topLeft.cloneFromEnd(330, length, 0.25f, Line.Type.CENTER).setRGBA(0,0,1,1);
            lineList.add(topRight);
            Renderer.get().render(topRight);
            Polygon corner = new Polygon(topLeft.getEndUp(),topRight.getStartUp(),topLeft.getEndOrg()).setRGBA(0,0,1,1);
            cornerList.add(corner);
            Renderer.get().render(corner);

            Line baseCPY = topRight.cloneFromEnd(-90,length,0.25f, Line.Type.CENTER);
            lineList.add(baseCPY);
            Renderer.get().render(baseCPY);

            Line botRight = baseCPY.cloneFromEnd(-150, length, 0.25f, Line.Type.CENTER);
            lineList.add(botRight);
            Renderer.get().render(botRight);
            Line botLeft = botRight.cloneFromEnd(150, length, 0.25f, Line.Type.CENTER);
            if(i == 0)botLeft.setRGBA(1,0,0,1);
            if(i == GameState.BOARD_SIZE - 1)baseCPY.setRGBA(1,0,0,1);
            lineList.add(botLeft);
            Renderer.get().render(botLeft);

            Polygon p = Polygon.fromLines(topLeft,topRight,baseCPY,botRight,botLeft,base).setRGBA(1,1,1,1);
            hexagonList.add(p);
            Renderer.get().render(p);

            base = baseCPY.mirrored();
        }

        Polygon corn = new Polygon(lineList.get(0).getEndUp(),lineList.get(1).getStartUp(),lineList.get(0).getEndOrg()).setRGBA(1,0,0,1);
        cornerList.add(corn);
        Renderer.get().render(corn);
        corn = new Polygon(lineList.get(0).getStartUp(),lineList.get(5).getEndUp(),lineList.get(0).getOrg()).setRGBA(1,0,0,1);
        cornerList.add(corn);
        Renderer.get().render(corn);

        Line st = lineList.get(lineList.size() - 4);
        corn = new Polygon(lineList.get(lineList.size() - 3).getStartUp(),lineList.get(lineList.size() - 3).getOrg(),st.getEndUp()).setRGBA(1,0,0,1);
        cornerList.add(corn);
        Renderer.get().render(corn);

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

                if(i == GameState.BOARD_SIZE - 2) {
                    botLeft.setRGBA(0,0,1,1);
                    botRight.setRGBA(0,0,1,1);

                    Polygon corner = new Polygon(botLeft.getStartUp(),botRight.getEndUp(),botRight.getEndOrg()).setRGBA(0,0,1,1);
                    cornerList.add(corner);
                    Renderer.get().render(corner);
                }

                if(innerBase == null) {
                    innerBase = botLeft.cloneFromEnd(90, length, 0.25f, Line.Type.CENTER).setRGBA(1,0,0,1);
                    lineList.add(innerBase);
                    Renderer.get().render(innerBase);
                    botLeft.setRGBA(1,0,0,1);

                    Polygon p = new Polygon(innerBase.getStartUp(),botLeft.getEndUp(),innerBase.getOrg()).setRGBA(1,0,0,1);
                    cornerList.add(p);
                    Renderer.get().render(p);
                }

                //if(i >= 1 && j == 9) {
                    //currentID--;
                //}

                Line l = lineList.get(currentID - ((i >= 1 && (j == 9 || j == 0))?3:2));
                if(i == 0)
                    l = j == 0?innerBase:innerBase.mirrored();
                Polygon p = Polygon.fromLines(right,botRight,botLeft,l,topLeft,topRight).setRGBA(1,1,1,1);
                hexagonList.add(p);
                Renderer.get().render(p);
                innerBase = right;
                currentID += i == 0?5:3;
            }

            Line topLeft = lineList.get(currentID - (i == 0?5:3));
            Line topRight = topLeft.cloneFromEnd(330, length, 0.25f, Line.Type.CENTER).setRGBA(1,0,0,1);
            lineList.add(topRight);
            Renderer.get().render(topRight);
            Line right = topRight.cloneFromEnd(-90, length, 0.25f, Line.Type.CENTER).setRGBA(1,0,0,1);
            lineList.add(right);


            Polygon corner = new Polygon(topRight.getEndUp(),right.getStartUp(),right.getOrg()).setRGBA(1,0,0,1);
            cornerList.add(corner);
            Renderer.get().render(corner);

            Line botRight = right.cloneFromEnd(-150, length, 0.25f, Line.Type.CENTER);
            lineList.add(botRight);
            Renderer.get().render(botRight);

            Renderer.get().render(right);

            Line botLeft = botRight.cloneFromEnd(150, length, 0.25f, Line.Type.CENTER);
            lineList.add(botLeft);
            Renderer.get().render(botLeft);

            innerBase = botLeft.cloneFromEnd(90, length, 0.25f, Line.Type.CENTER);
            lineList.add(innerBase);
            //Renderer.get().render(innerBase);

            if(i == GameState.BOARD_SIZE - 2) {
                botLeft.setRGBA(0,0,1,1);
                botRight.setRGBA(0,0,1,1);

                Polygon botCorn = new Polygon(botLeft.getOrg(),botLeft.getStartUp(),botRight.getEndUp()).setRGBA(0,0,1,1);
                cornerList.add(botCorn);
                Renderer.get().render(botCorn);

                Polygon rightCorn = new Polygon(right.getEndUp(),right.getEndOrg(),botRight.getStartUp()).setRGBA(1,0,0,1);
                cornerList.add(rightCorn);
                Renderer.get().render(rightCorn);
            }

            Polygon p = Polygon.fromLines(topLeft,topRight,right,botRight,botLeft,innerBase).setRGBA(1,1,1,1);
            hexagonList.add(p);
            Renderer.get().render(p);

            currentID += i == 0?2:-1;
        }

        topOffset = lineList.getLast().getStart().y;
        HIDInput.activateListener(this);
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
     * this adds a {@link UIPlayer} for the instance e.g. the ones that can make moves on this board
     * @param uiPlayer the UIPlayer
     */
    public static void addPlayer(UIPlayer uiPlayer) {
        get().playerList.add(uiPlayer);
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
        if(gameState.getPiece(move).getColor() == Piece.Color.RED)hex.setRGBA(1,0,0,1);
        else hex.setRGBA(0,0,1,1);
        //Renderer.get().render(hex);
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
        Position pos = Util.convertToGameCords(x, y);
        Move move = new Move(pos);
        if(isRemote){
            PacketByteBuf buf = PacketByteBuf.create();
            buf.writeLong(System.currentTimeMillis());
            buf.writeInt(pos.getIndex());
            Client.send2Server("makeMove",buf);
        }else playerList.forEach(uiPlayer -> uiPlayer.makeMove(move));
    }

    public List<UIPlayer> getLocalPlayers() {
        return playerList;
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

    public void endGame(){
        hexagonList.forEach(hex -> hex.setA(0.5f));
        lineList.forEach(line -> line.setA(0.5f));
        cornerList.forEach(corner -> corner.setA(0.5f));

        playerList.clear();

        running = true;

        HIDInput.deactivateListener(this);
    }

    public void forceEnd(){
        HexClient.forceStop();
        HexServer.forceStop();
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setRemote(boolean remote) {
        isRemote = remote;
    }

    public boolean isRunning(){
        return running;
    }
}
