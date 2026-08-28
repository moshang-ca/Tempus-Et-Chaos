package org.moshang.tempusetchaos.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.moshang.tempusetchaos.TempusEtChaos;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class EntropyDiffusionHandler {
    public static final int DIFFUSION_INTERVAL = 120;
    public static final float DIFFUSION_RATE = .05f;
    public static final float TRANSFER_EPSILON = .005f;

    public static void diffusion(ServerLevel level) {
        EntropyWorldData data = EntropyWorldData.get(level);
        data.cleanupInvalid();
        Map<ChunkPos, Float> concentrations = data.getConcentrations();
        if (concentrations.isEmpty()) return;

        Map<ChunkPos, Float> transfers = new HashMap<>();
        for (var entry : concentrations.entrySet()) {
            ChunkPos pos = entry.getKey();
            float value = entry.getValue();
            if (value <= 0) continue;

            for (ChunkPos neighbor : getNeighbors(pos)) {
                float nearingValue = data.getConcentration(neighbor);
                float transfer = (value - nearingValue) * DIFFUSION_RATE;
                if (transfer > TRANSFER_EPSILON) {
                    addTransfer(transfers, pos, -transfer);
                    addTransfer(transfers, neighbor, transfer);
                }
            }
        }

        for (var entry : transfers.entrySet()) {
            data.addConcentration(entry.getKey(), entry.getValue());
            System.out.println(data.getConcentration(entry.getKey()));
        }
    }

    private static ChunkPos[] getNeighbors(ChunkPos cPos) {
        return new ChunkPos[] {
                new ChunkPos(cPos.x, cPos.z + 1),
                new ChunkPos(cPos.x, cPos.z - 1),
                new ChunkPos(cPos.x + 1, cPos.z),
                new ChunkPos(cPos.x - 1, cPos.z)
        };
    }

    private static void addTransfer(Map<ChunkPos, Float> transfers, ChunkPos cPos, float amount) {
        transfers.put(cPos, transfers.getOrDefault(cPos, 0.f) + amount);
    }

    @EventBusSubscriber(modid = TempusEtChaos.MODID)
    public static class Listener {
        private static final Map<ServerLevel, Integer> timer = new WeakHashMap<>();

        @SubscribeEvent
        public static void onLevelTick(LevelTickEvent.Post event) {
            if (event.getLevel() instanceof ServerLevel level) {
                int count = timer.getOrDefault(level, 0);
                count++;

                if (count >= DIFFUSION_INTERVAL) {
                    diffusion(level);
                    count = 0;
                }
                timer.put(level, count);
            }
        }
    }
}
