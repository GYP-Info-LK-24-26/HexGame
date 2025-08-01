package de.hexgame.nn.training;

import de.hexgame.nn.util.CircularFifoQueue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.tensorflow.framework.data.Dataset;
import org.tensorflow.proto.Example;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class ExperienceBuffer {
    private final CircularFifoQueue<Example> dataSetBuffer;

    public ExperienceBuffer(int size) {
        dataSetBuffer = new CircularFifoQueue<>(size);
    }

    public synchronized void load(File file) {
        try {
            Example dataSet = new org.nd4j.linalg.dataset.Example();
            dataSet.load(file);
            dataSetBuffer.addAll(dataSet.asList());
        } catch (IOException e) {
            log.error("Error while loading experience buffer", e);
        }
        log.info("Loaded {} experiences", dataSetBuffer.size());
    }

    public synchronized void save(File file) {
        try {
            org.nd4j.linalg.dataset.Example.merge(dataSetBuffer).save(file);
        } catch (IOException e) {
            log.error("Error while saving experience buffer to file", e);
        }
    }

    public synchronized void add(Example dataSet) {
        dataSetBuffer.add(dataSet);
    }

    public synchronized Dataset sample(int count) {
        List<Dataset> samples = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(dataSetBuffer.size());
            samples.add(dataSetBuffer.get(randomIndex));
        }
        return org.nd4j.linalg.dataset.Example.merge(samples);
    }

    public synchronized int size() {
        return dataSetBuffer.size();
    }
}
