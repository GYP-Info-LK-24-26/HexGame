package de.hexgame.logic;

import java.io.Serializable;

public class UnionFind implements Serializable, Cloneable {
    private int[] parent;
    private int[] rank;

    public UnionFind(int size) {
        parent = new int[size];
        for (int i = 0; i < size; i++) parent[i] = i;
        rank = new int[size];
    }

    public void union(int x, int y) {
        x = find(x);
        y = find(y);
        if (x == y) return;
        if (rank[x] < rank[y]) {
            int temp = x;
            x = y;
            y = temp;
        }
        parent[y] = x;
        if (rank[x] == rank[y]) {
            rank[x]++;
        }
    }

    public int find(int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    @Override
    public UnionFind clone() {
        try {
            UnionFind clone = (UnionFind) super.clone();
            clone.parent = parent.clone();
            clone.rank = rank.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
