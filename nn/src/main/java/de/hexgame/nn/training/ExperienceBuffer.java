package de.hexgame.nn.training;

import de.hexgame.logic.GameState;
import de.hexgame.nn.Model;
import de.hexgame.nn.util.CircularFifoQueue;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class ExperienceBuffer {
    private final CircularFifoQueue<Sample> sampleBuffer;
    private final ReadWriteLock lock;

    public ExperienceBuffer(int size) {
        sampleBuffer = new CircularFifoQueue<>(size);
        lock = new ReentrantReadWriteLock();
    }

    public void load(File file) {
        lock.writeLock().lock();
        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))))) {
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                sampleBuffer.add((Sample) in.readObject());
            }
            log.info("Loaded {} samples", sampleBuffer.size());
        } catch (IOException | ClassNotFoundException e) {
            log.error("Error while loading experience buffer", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void save(File file) {
        lock.readLock().lock();
        try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file))))) {
            out.writeInt(sampleBuffer.size());
            for (Sample s : sampleBuffer) {
                out.writeObject(s);
            }
            log.info("Saved {} samples", sampleBuffer.size());
        } catch (IOException e) {
            log.error("Error while saving experience buffer", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void add(Sample sample) {
        lock.writeLock().lock();
        sampleBuffer.add(sample);
        lock.writeLock().unlock();
    }

    public void sample(List<Sample> samples, int count) {
        lock.readLock().lock();
        try {
            for (int i = 0; i < count; i++) {
                int randomIndex = ThreadLocalRandom.current().nextInt(sampleBuffer.size());
                samples.add(sampleBuffer.get(randomIndex));
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        int size = sampleBuffer.size();
        lock.readLock().unlock();
        return size;
    }

    public record Sample(GameState gameState, Model.Output targetOutput) implements Serializable {
    }
}
