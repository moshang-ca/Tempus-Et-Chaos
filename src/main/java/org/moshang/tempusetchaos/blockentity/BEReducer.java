package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.registry.TECBlockEntities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BEReducer extends BaseChrononNodeBlockEntity {
    private static final int BASE_CONSUMPTION = 0;
    private static final int MAX_RED_ENTITY = 32;

    private final Set<ResourceLocation> blacklist = new HashSet<>();
    private final List<Entity> entityCache = new ArrayList<>();
    private final int randomUpdateTick;
    private final AABB area;

    @Getter
    private int reduceMultiplier = 8;
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
        assert level != null;
        tickCounter++;
        if (tickCounter % 20 == randomUpdateTick) {
            entityCache.clear();
            entityCache.addAll(level.getEntities((Entity) null, area, entity -> {
                ResourceLocation entityTypeName = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                return !blacklist.contains(entityTypeName);
            }));
        }
        if (innerNetwork != null) {
            long extracted = innerNetwork.extractChronon(consumed, false);
            if (extracted == consumed) {
                decelerateEntities();
            }
        }
    }

    public void setReduceMultiplier(int reduceMultiplier) {
        this.reduceMultiplier = Mth.clamp(reduceMultiplier, 2, 4);
        this.consumed = (int) (BASE_CONSUMPTION * Math.pow(2, reduceMultiplier - 2));
    }

    private void decelerateEntities() {
        for (int i = 0; i < entityCache.size() && i < MAX_RED_ENTITY; ++i) {
            Entity entity = entityCache.get(i);
            if (!entity.isAlive()) return;
            if (entity instanceof LivingEntity living) {
                living.getPersistentData().putInt("chronon_slowdown", reduceMultiplier);
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
}
