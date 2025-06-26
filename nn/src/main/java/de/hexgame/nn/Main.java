package de.hexgame.nn;

public class Main {
    public static void main(String[] args) {
        Model model = new Model();
        Trainer trainer = new Trainer(model);
        trainer.run();
    }
}
