package de.hexgame.uifx.board;

import de.hexgame.algorithm.mcts.ICEResult;
import de.hexgame.algorithm.mcts.InferiorCellEngine;
import de.hexgame.logic.*;
import de.hexgame.uifx.NavigationManager;
import de.hexgame.uifx.TranslationManager;
import de.hexgame.uifx.gui.MainMenuPane;
import de.hexgame.uifx.gui.Styles;
import de.hexgame.uifx.networking.HexClient;
import de.hexgame.uifx.networking.HexServer;
import de.hexgame.uifx.networking.packets.BoardChangeHandler;
import de.hexgame.uifx.networking.packets.ClientNetworkCallback;
import de.hexgame.uifx.networking.packets.GameEndHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class HexBoardController implements PlayerMoveListener, ClientNetworkCallback {
    private static HexBoardController instance;
    @Getter
    private HexBoardCanvas canvas;
    @Getter
    private StackPane boardPane;
    @Getter
    private GameState gameState;
    private Game game;
    private Player playerA;
    private Player playerB;
    @Getter
    private final List<UIPlayer> localPlayers = new ArrayList<>();
    @Setter
    @Getter
    private boolean isRemote = false;
    @Getter
    private boolean running = false;
    private boolean awaitingLocalInput = false;
    private boolean gameOver = false;
    private static final int MIN_TIME_PER_TURN = 100;
    private long lastTimeRun = 0;
    private boolean showICE = false;

    private HexBoardController() {}

    public static HexBoardController get() {
        if (instance == null) instance = new HexBoardController();
        return instance;
    }

    public StackPane createBoardPane() {
        boardPane = new StackPane();
        boardPane.setStyle("-fx-background-color: #1a1a2e;");
        canvas = new HexBoardCanvas(900, 700);
        boardPane.getChildren().add(canvas);
        canvas.setOnMouseClicked(this::onMouseClicked);
        canvas.setOnMouseMoved(this::onMouseMoved);
        canvas.setOnMouseExited(e -> {
            canvas.setHoverPosition(null);
            canvas.redraw();
        });

        CheckBox iceToggle = new CheckBox("ICE");
        iceToggle.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 13;");
        iceToggle.setSelected(showICE);
        iceToggle.selectedProperty().addListener((obs, old, val) -> {
            showICE = val;
            updateICEOverlay();
        });
        StackPane.setAlignment(iceToggle, Pos.TOP_RIGHT);
        StackPane.setMargin(iceToggle, new Insets(8, 12, 0, 0));
        boardPane.getChildren().add(iceToggle);

        return boardPane;
    }

    public void startGame(Player first, Player second, boolean withRemote, HexServer server) {
        localPlayers.clear();
        if (first instanceof UIPlayer) localPlayers.add((UIPlayer) first);
        if (second instanceof UIPlayer) localPlayers.add((UIPlayer) second);

        this.playerA = first;
        this.playerB = second;

        gameState = new GameState();
        canvas.setGameState(gameState);

        game = new Game(gameState, first, second);
        game.addPlayerMoveListener(this);

        if (withRemote && server != null) {
            game.addPlayerMoveListener(new BoardChangeHandler(gameState, server));
            game.addPlayerWinListener(new GameEndHandler(server));
        }

        game.addPlayerWinListener(player -> Platform.runLater(() -> showWinner(player.getName())));

        running = true;
        gameOver = false;
        awaitingLocalInput = false;
        canvas.setInputEnabled(false);
        canvas.setWinnerLabel(null);
        canvas.setHoverPosition(null);
        canvas.setIceResult(null);
        updateLabel();
        canvas.redraw();

        NavigationManager.get().navigateTo(boardPane);
        game.asThread().start();

        if (withRemote && server != null) {
            server.sendToAll("start", new byte[0]);
        }
    }

    public void restartGame() {
        if (playerA == null || playerB == null) return;

        // Re-create UIPlayers for a fresh game (old ones may be stuck in wait)
        Player newA = playerA instanceof UIPlayer ? new UIPlayer() : playerA;
        Player newB = playerB instanceof UIPlayer ? new UIPlayer() : playerB;

        startGame(newA, newB, isRemote, isRemote ? HexServer.getInstance() : null);
    }

    private void onMouseClicked(MouseEvent event) {
        if (gameOver) return;
        Position pos = canvas.getGeometry().screenToBoard(event.getX(), event.getY());
        if (!pos.isValid()) return;
        Move move = new Move(pos);

        if (isRemote) {
            HexClient client = HexClient.getInstance();
            if (client != null) {
                client.sendMakeMove(pos);
            }
        } else {
            localPlayers.forEach(p -> p.makeMove(move));
        }
    }

    private void onMouseMoved(MouseEvent event) {
        Position pos = canvas.getGeometry().screenToBoard(event.getX(), event.getY());
        if (!pos.isValid()) {
            canvas.setHoverPosition(null);
        } else {
            canvas.setHoverPosition(pos);
        }
        canvas.redraw();
    }

    @Override
    public void onPlayerMove(Position move) {
        long delta = System.currentTimeMillis() - lastTimeRun;
        if (delta < MIN_TIME_PER_TURN) {
            try {
                Thread.sleep(MIN_TIME_PER_TURN - delta);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        lastTimeRun = System.currentTimeMillis();
        Platform.runLater(() -> {
            updateLabel();
            updateICEOverlay();
        });
    }

    @Override
    public void onPlayerPreMove(Player player) {
        boolean isLocal = localPlayers.contains(player);
        Platform.runLater(() -> {
            awaitingLocalInput = isLocal;
            canvas.setInputEnabled(isLocal);
            TranslationManager t = TranslationManager.get();
            String label = player == playerA ? t.translate("first") : t.translate("second");
            int color = gameState.getSideToMove();
            canvas.setCurrentPlayerLabel(label + " (" + color + ")");
            canvas.redraw();
        });
    }

    // --- ClientNetworkCallback implementation ---

    @Override
    public void onBoardChange(Position pos, int color) {
        gameState.setPiece(pos, color);
        updateICEOverlay();
    }

    @Override
    public void onBoardClear() {
        endGame();
    }

    @Override
    public void onGameStart() {
        createBoardPane();
        setGameState(new GameState());
        running = true;
        gameOver = false;
        canvas.setInputEnabled(true);
        canvas.setWinnerLabel(null);
        canvas.setHoverPosition(null);
        canvas.redraw();
        NavigationManager.get().navigateTo(boardPane);
    }

    @Override
    public void onConnectionState(boolean connected) {
        // Handled by ConnectPane directly; this is a no-op for the controller.
    }

    @Override
    public void onGameEnd(String playerName) {
        showWinner(playerName);
    }

    // --- end ClientNetworkCallback ---

    private void updateLabel() {
        if (gameState != null) {
            int color = gameState.getSideToMove();
            canvas.setCurrentPlayerLabel(String.valueOf(color));
        }
    }

    private void updateICEOverlay() {
        if (showICE && gameState != null && !gameState.isFinished()) {
            ICEResult result = InferiorCellEngine.computeICE(gameState);
            canvas.setIceResult(result);
        } else {
            canvas.setIceResult(null);
        }
        canvas.redraw();
    }

    public void showWinner(String playerName) {
        gameOver = true;
        awaitingLocalInput = false;
        canvas.setInputEnabled(false);

        TranslationManager t = TranslationManager.get();
        String message;
        if (localPlayers.size() == 1) {
            boolean won = playerName.equals(localPlayers.getFirst().getName());
            message = won ? t.translate("won") : t.translate("loss");
        } else {
            message = playerName + " " + t.translate("player_won");
        }
        canvas.setWinnerLabel(message);
        canvas.redraw();

        // Add buttons over the board
        Button mainMenuBtn = Styles.styledButton(t.translate("main_menu"));
        mainMenuBtn.setOnAction(e -> {
            endGame();
            HexClient.stop();
            HexServer.stop();
            NavigationManager.get().navigateTo(new MainMenuPane());
        });

        Button reRunBtn = Styles.styledButton(t.translate("re_run"));
        reRunBtn.setOnAction(e -> {
            boardPane.getChildren().removeIf(n -> n instanceof VBox);
            endGame();
            restartGame();
        });

        VBox buttons = new VBox(10, mainMenuBtn, reRunBtn);
        buttons.setAlignment(Pos.BOTTOM_CENTER);
        buttons.setStyle("-fx-padding: 20;");
        buttons.setPickOnBounds(false);
        boardPane.getChildren().add(buttons);
    }

    public void endGame() {
        localPlayers.clear();
        running = false;
        gameOver = false;
    }

    public void forceEnd() {
        try {
            if (game != null) {
                game.terminate();
                localPlayers.forEach(UIPlayer::end);
                HexServer.finish();
                game.asThread().join();
            }
            HexClient.forceStop();
            HexServer.forceStop();
            game = null;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        if (canvas != null) canvas.setGameState(gameState);
    }
}
