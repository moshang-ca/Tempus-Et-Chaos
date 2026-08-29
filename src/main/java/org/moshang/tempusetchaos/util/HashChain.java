package org.moshang.tempusetchaos.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class HashChain<T> {
    @FunctionalInterface
    public interface Hasher<T> {
        int hash(T t);
    }

    private final Int2ObjectOpenHashMap<Node<T>> nodeMap = new Int2ObjectOpenHashMap<>();
    private final Hasher<T> hasher;

    public HashChain(Hasher<T> hasher) {
        this.hasher = hasher;
    }

    @SafeVarargs
    public final void addChain(T... chain) {
        addChain(Arrays.asList(chain));
    }

    public void addChain(List<T> chain) {
        Node<T> prev = null;
        for (T t : chain) {
            int key = hasher.hash(t);
            Node<T> cur = nodeMap.computeIfAbsent(key, k -> new Node<>(t));
            if (cur.value != t)
                nodeMap.put(key, new Node<>(t));    // If the key already exists (in some exceptional cases), we will cover it by default.
            if (prev != null) {
                prev.next = cur;
                cur.prev = prev;
            }
            prev = cur;
        }
    }

    @Nullable
    public Node<T> find(T value) {
        return nodeMap.getOrDefault(hasher.hash(value), null);
    }

    public static class Node<T> {
        public final T value;
        @Nullable public Node<T> prev;
        @Nullable public Node<T> next;

        public Node(T value) {
            this.value = value;
        }
    }
}
