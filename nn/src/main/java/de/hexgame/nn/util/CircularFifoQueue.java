package de.hexgame.nn.util;

import java.util.*;

public class CircularFifoQueue<T> implements Queue<T> {
    private final T[] array;
    private int head;
    private int tail;

    @SuppressWarnings("unchecked")
    public CircularFifoQueue(int size) {
        array = (T[]) new Object[size];
    }

    @Override
    public int size() {
        return head <= tail ? tail - head : array.length - head + tail;
    }

    @Override
    public boolean isEmpty() {
        return head == tail;
    }

    @Override
    public boolean contains(Object o) {
        for (int i = head; i < tail; i = successor(i)) {
            if (Objects.equals(array[i], o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return head == tail ? Collections.emptyIterator() : new Iterator<>() {
            int currentIndex = head;
            @Override
            public boolean hasNext() {
                return currentIndex < tail;
            }

            @Override
            public T next() {
                T result = array[currentIndex];
                currentIndex = successor(currentIndex);
                return result;
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public T[] toArray() {
        T[] result = (T[]) new Object[size()];
        int lastElement = predecessor(tail);
        if (lastElement >= head) {
            System.arraycopy(array, head, result, 0, result.length);
        } else if (tail < head) {
            System.arraycopy(array, head, result, 0, array.length - head);
            System.arraycopy(array, 0, result, result.length - head, tail);
        }
        return result;
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(T t) {
        if (!offer(t)) {
            throw new IllegalStateException("Queue is full");
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T o : c) {
            add(o);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        head = tail = 0;
        Arrays.fill(array, null);
    }

    @Override
    public boolean offer(T t) {
        if (size() == array.length) {
            return false;
        }

        array[tail] = t;
        tail = successor(tail);
        return true;
    }

    @Override
    public T remove() {
        T result = poll();
        if (result == null) {
            throw new IllegalStateException("Queue is empty");
        }
        return result;
    }

    @Override
    public T poll() {
        if (head == tail) {
            return null;
        }

        T result = array[head];
        array[head] = null;
        head = successor(head);
        return result;
    }

    @Override
    public T element() {
        return null;
    }

    @Override
    public T peek() {
        if (head == tail) {
            return null;
        }

        return array[head];
    }

    private int predecessor(int index) {
        return index == 0 ? array.length - 1 : index - 1;
    }

    private int successor(int index) {
        return index == array.length - 1 ? 0 : index + 1;
    }
}
