package de.hexgame.uifx;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class NavigationManager {
    private static NavigationManager instance;
    private Stage stage;
    private Scene scene;

    private NavigationManager() {}

    public static NavigationManager get() {
        if (instance == null) instance = new NavigationManager();
        return instance;
    }

    public void init(Stage stage) {
        this.stage = stage;
        Pane root = new Pane();
        scene = new Scene(root, 900, 700);
        scene.getStylesheets().add("style.css");
        stage.setScene(scene);
    }

    public void navigateTo(Pane pane) {
        scene.setRoot(pane);
        pane.requestFocus();
    }

    public Stage getStage() {
        return stage;
    }

    public Scene getScene() {
        return scene;
    }
}
