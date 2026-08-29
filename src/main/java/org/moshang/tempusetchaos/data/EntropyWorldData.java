package org.moshang.tempusetchaos.data;

import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EntropyWorldData extends SavedData {
    private static final float EPSILON = 1.f;

    @Getter
    private final Map<ChunkPos, Float> concentrations = new HashMap<>();

    public static EntropyWorldData create() {
        return new EntropyWorldData();
    }

    public static EntropyWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(
                EntropyWorldData::create,
                EntropyWorldData::load
        ), "entropy_data");
    }

    public static EntropyWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
        EntropyWorldData data = create();
        CompoundTag concentrationsTag = tag.getCompound("concentrations");
        for (String key : concentrationsTag.getAllKeys()) {
            ChunkPos cPos = new ChunkPos(Long.parseLong(key));
            data.setConcentration(cPos, concentrationsTag.getInt(key));
        }
        return data;
    }

    public static float getConcentration(ServerLevel level, ChunkPos pos) {
        return EntropyWorldData.get(level).getConcentration(pos);
    }

    private EntropyWorldData() {}

    public Map<ChunkPos, Float> snapshot() {
        return new HashMap<>(concentrations);
    }

    public float getConcentration(ChunkPos cpos) {
        return concentrations.getOrDefault(cpos, 0.f);
    }

    public Set<ChunkPos> getEntropiedChunks() {
        return concentrations.keySet();
    }

    public void setConcentration(ChunkPos cPos, float concentration) {
        concentrations.put(cPos, Mth.clamp(concentration, 0.f, 100.f));
        setDirty();
    }

    public void addConcentration(ChunkPos cPos, float increment) {
        float res = getConcentration(cPos) + increment;
        setConcentration(cPos, res < EPSILON ? 0 : res);
    }

    public void cleanupInvalid() {
        concentrations.entrySet().removeIf(entry -> entry.getValue() <= EPSILON);
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag compoundTag, @NotNull HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        for (var entry : concentrations.entrySet()) {
            float value = entry.getValue();
            if (value > 0)
                tag.putFloat(String.valueOf(entry.getKey().toLong()), entry.getValue());
        }
        compoundTag.put("concentrations", tag);
        return compoundTag;
    }
}
