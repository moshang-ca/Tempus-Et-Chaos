package org.moshang.tempusetchaos.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public class HashList {
    private final Int2ObjectOpenHashMap<Node> nodeMap = new Int2ObjectOpenHashMap<>();

    public HashList() {}

    public void addNode(Block value) {
        int hash = BuiltInRegistries.BLOCK.getId(value);
        Node node = new Node(value, hash);
        nodeMap.put(hash, node);
    }

    public static class Node {
        public final Block block;
        @Getter
        private final int hashKey;
        public Node prev;
        public Node next;

        public Node(Block block, int hashKey) {
            this.block = block;
            this.hashKey = hashKey;
        }
    }
}
