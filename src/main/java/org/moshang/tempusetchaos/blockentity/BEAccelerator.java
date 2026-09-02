package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.registry.TECBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
public class BEAccelerator extends BaseChrononNodeBlockEntity {
    @Getter
    private static final Set<ResourceLocation> DEFAULT_BLACKLIST = new HashSet<>();
    private static final int BASE_CONSUMPTION = 5;     // 5 ch/tick
    private static final int MAX_ACC_ENTITY = 32;     // 32 entity/acc (in default)

    public static void initDefault() {

    }

    private static int randomTicks;

    @Getter
    private final Set<ResourceLocation> blacklist = new HashSet<>();
    private final List<Entity> entityCache = new ArrayList<>();
    private final AABB area;
    @Getter
    private int accelerateMultiplier = 8;
    @Getter
    private int consumed = 0;
    private long tickCounter = 0;

    public BEAccelerator(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.ACCELERATOR_BE.get(), pos, blockState, 2400);
        BlockPos start = getBlockPos().offset(-1, 1, -1);
        BlockPos end = getBlockPos().offset(1, 3, 1);
        area = new AABB(start.getCenter(), end.getCenter());
    }

    @Override
    public void serverTick() {
        super.serverTick();
        assert level != null;
        randomTicks = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        tickCounter++;
        if (tickCounter % 20 == 1) {
            entityCache.clear();
            entityCache.addAll(level.getEntities((Entity) null, area, entity -> {
                ResourceLocation entityTypeName = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                return !(DEFAULT_BLACKLIST.contains(entityTypeName) || blacklist.contains(entityTypeName));
            }));
        }
        if (innerNetwork != null) {
           long extract = innerNetwork.extractChronon(getConsumed(), false);
           if (extract == getConsumed()) {
                accelerateBlocks();
                accelerateEntities();
           }
        }
    }

    @SuppressWarnings("unchecked")
    private void accelerateBlocks() {
        int extraTicks = accelerateMultiplier - 1;
        BlockPos.betweenClosedStream(area).forEach(pos -> {
            assert level != null;
            BlockState state = level.getBlockState(pos);
            ResourceLocation blockName = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (DEFAULT_BLACKLIST.contains(blockName) || blacklist.contains(blockName)) return;
            if (state.isRandomlyTicking() && level.random.nextInt(randomTicks) < Math.min(extraTicks, randomTicks)) {
                state.randomTick((ServerLevel) level, pos, level.random);
            } else  {
                BlockEntity be = level.getBlockEntity(pos);
                if (be == null) return;
                BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) state.getTicker(level, be.getType());
                if (be.isRemoved() || ticker == null) return;
                for (int i = 0; i < extraTicks; ++i) {
                    ticker.tick(level, pos, state, be);
                }
            }
        });
    }

    private void accelerateEntities() {
        int extraTicks = accelerateMultiplier - 1;
        assert level != null;
        for (int i = 0; i < entityCache.size() && i < MAX_ACC_ENTITY; ++i) {
            Entity entity = entityCache.get(i);
            if (entity instanceof Player) continue;
            for (int j = 0; j < extraTicks; ++j) {
                if (!entity.isAlive()) break;
                entity.tick();
            }
        }
    }

    public void setAccelerateMultiplier(int accelerateMultiplier) {
        this.accelerateMultiplier = Mth.clamp(accelerateMultiplier, 2, 4);
        this.consumed = (int) (BASE_CONSUMPTION * Math.pow(3, accelerateMultiplier));
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.accelerateMultiplier = Mth.clamp(tag.getInt("acc_multiplier"), 2, 4);

        ListTag blacklistTag = tag.getList("blacklist", StringTag.TAG_STRING);
        for (int i = 0; i < blacklistTag.size(); ++i) {
            ResourceLocation blacked = ResourceLocation.tryParse(blacklistTag.getString(i));
            if (blacked != null) {
                blacklist.add(blacked);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("acc_multiplier", this.accelerateMultiplier);

        ListTag blacklistTag = new ListTag();
        for (ResourceLocation blacked : blacklist) {
            blacklistTag.add(StringTag.valueOf(blacked.toString()));
        }
        tag.put("blacklist", blacklistTag);
    }
}
