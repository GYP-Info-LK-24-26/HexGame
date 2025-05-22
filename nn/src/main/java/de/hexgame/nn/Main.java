package de.hexgame.nn;

import de.hexgame.logic.Game;
import de.hexgame.logic.GameBoard;
import de.hexgame.logic.Player;
import de.hexgame.logic.RandomPlayer;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import static de.hexgame.logic.GameBoard.BOARD_SIZE;

public class Main {
    public static void main(String[] args) {
        GameBoard gameBoard = new GameBoard();
        Player playerA = new RandomPlayer();
        Player playerB = new RandomPlayer();
        Game game = new Game(gameBoard, playerA, playerB);
        game.start();

        ComputationGraphConfiguration config = new NeuralNetConfiguration.Builder()
                .updater(new Adam(1e-3))
                .graphBuilder()
                .addInputs("input")
                .setInputTypes(InputType.convolutional(BOARD_SIZE, BOARD_SIZE, 4))
                .addLayer("conv1", new ConvolutionLayer.Builder(3, 3)
                        .nOut(64)
                        .padding(1, 1)
                        .activation(Activation.RELU)
                        .build(), "input")
                .addLayer("conv2", new ConvolutionLayer.Builder(3, 3)
                        .nOut(64)
                        .padding(1, 1)
                        .activation(Activation.RELU)
                        .build(), "conv1")

                .addLayer("policyConv", new ConvolutionLayer.Builder(1, 1)
                        .nOut(2)
                        .activation(Activation.RELU)
                        .build(), "conv2")
                .addLayer("policyOut", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .nOut(BOARD_SIZE * BOARD_SIZE + 1)
                        .activation(Activation.SOFTMAX)
                        .build(), "policyConv")

                .addLayer("valueConv", new ConvolutionLayer.Builder(1, 1)
                        .nOut(1)
                        .activation(Activation.RELU)
                        .build(), "conv2")
                .addLayer("valueOut", new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nOut(1)
                        .activation(Activation.TANH)
                        .build(), "valueConv")

                .setOutputs("policyOut", "valueOut")
                .build();

        ComputationGraph graph = new ComputationGraph(config);
        graph.init();

        INDArray input = Nd4j.create(1, 4, BOARD_SIZE, BOARD_SIZE);
        INDArray policyTarget = Nd4j.create(1, BOARD_SIZE * BOARD_SIZE);
        INDArray valueTarget = Nd4j.create(1, 1);

        MultiDataSet dataSet = new MultiDataSet(new INDArray[]{input}, new INDArray[]{policyTarget, valueTarget});
    }


}
