package de.hexgame.nn;

public class Main {
    public static void main(String[] args) {
        Model model = new Model();
        model.start();
        Trainer trainer = new Trainer(model);
        trainer.run();
    }
}
