package org.moshang.tempusetchaos.blockentity;

import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.util.MatchSet;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class BEReducer extends BaseChrononNodeBlockEntity {
    private static final int BASE_CONSUMPTION = 5;
    private static final int MAX_RED_ENTITY = 32;
    private static final Logger LOGGER = LogUtils.getLogger();

    @Getter
    private final MatchSet blacklist = new MatchSet();
    private final List<Entity> entityCache = new ArrayList<>();
    private final int randomUpdateTick;
    private final AABB area;

    @Getter
    private int reduceMultiplier = 1;
    @Getter
    private int consumed = BASE_CONSUMPTION;
    private int tickCounter = 0;

    public BEReducer(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.REDUCER_BE.get(), pos, blockState, 2400);
        BlockPos start = getBlockPos().offset(-1, 1, -1);
        BlockPos end = getBlockPos().offset(1, 3, 1);
        area = new AABB(start.getCenter(), end.getCenter());
        if (level != null)
            randomUpdateTick = level.getRandom().nextIntBetweenInclusive(0, 20);
        else randomUpdateTick = 1;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (reduceMultiplier == 1) return;
        assert level != null;
        tickCounter++;
        if (tickCounter % 20 == randomUpdateTick) {
            entityCache.clear();
            entityCache.addAll(level.getEntities((Entity) null, area, entity -> {
                EntityType<?> type = entity.getType();
                ResourceLocation entityTypeName = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                return !blacklist.matchEntity(entityTypeName, type);
            }));
        }
        if (innerNetwork != null) {
            long extracted = innerNetwork.extractChronon(consumed, false);
            if (extracted == consumed && tickCounter % 40 == 0) {
                decelerateEntities();
            }
        }
    }

    public void setReduceMultiplier(int reduceMultiplier) {
        this.reduceMultiplier = Mth.clamp(reduceMultiplier, 1, 4);
        this.consumed = (int) (BASE_CONSUMPTION * Math.pow(2, reduceMultiplier - 2));
    }

    private void decelerateEntities() {
        assert level != null;
        for (int i = 0; i < entityCache.size() && i < MAX_RED_ENTITY; ++i) {
            Entity entity = entityCache.get(i);
            if (!entity.isAlive()) continue;
            if (entity instanceof LivingEntity living) {
                living.getPersistentData().putInt("chronon_slowdown", reduceMultiplier);
                living.getPersistentData().putLong("chronon_slowdown_ts", level.getGameTime());
            } else {
                Vec3 motion = entity.getDeltaMovement();
                double factor = 1. / reduceMultiplier;
                entity.setDeltaMovement(motion.multiply(factor, factor, factor));
            }
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.reduceMultiplier = Mth.clamp(tag.getInt("red_multiplier"), 2, 4);
        MatchSet.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("blacklist"))
                .resultOrPartial(error -> LOGGER.warn("Failed to load blacklist: {}", error))
                .ifPresent(loaded -> loaded.ordered().forEach(this.blacklist::add));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("red_multiplier", this.reduceMultiplier);
        MatchSet.CODEC.encodeStart(NbtOps.INSTANCE, this.blacklist)
                .resultOrPartial(error -> LOGGER.warn("Failed to save blacklist: {}", error))
                .ifPresent(nbt -> tag.put("blacklist", nbt));
    }

    public void addBlacklist(MatchSet.Entry entry) {
        if (blacklist.add(entry)) setChanged();
    }

    public void removeBlacklist(MatchSet.Entry entry) {
        if (blacklist.remove(entry)) setChanged();
    }
}
