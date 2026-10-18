package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.api.IMatchable;
import org.moshang.tempusetchaos.menu.MenuAccelerator;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.util.MatchSet;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@ParametersAreNonnullByDefault
public class BEAccelerator extends BaseChrononNodeBlockEntity implements IMatchable, MenuProvider {
    private static final Set<MatchSet.Entry> DEFAULT_BLACKLIST = new HashSet<>();
    private static final int[] CONSUMPTION = new int[1025];
    private static final int BASE_CONSUMPTION = 5;     // 5 ch/tick
    private static final int MAX_ACC_ENTITY = 32;      // 32 entity/acc (in default)
    public static final int MIN_MULTIPLIER = 2;
    public static final int MAX_MULTIPLIER = 4;

    public static void initDefault() {

    }

    static {
        for (int i = 1; i <= 1024; ++i) {
            CONSUMPTION[i] = (int) (BASE_CONSUMPTION * (i * i + StrictMath.pow(i, 1.8) * (StrictMath.log(i) * 1.4426950408889634)));
        }
    }

    @Getter
    private final MatchSet matchSet = new MatchSet();
    private final List<Entity> entityCache = new ArrayList<>();
    private final AABB area;
    @Getter
    private boolean blacklistMode = true;
    @Getter
    private int accelerateMultiplier = MIN_MULTIPLIER;
    @Getter
    private int consumed = consumptionOf(MIN_MULTIPLIER);
    private long lastRefreshTick = 0;

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
        lastRefreshTick++;
        if (level.getGameTime() % 20 == 1 && level.getGameTime() != lastRefreshTick) {
            entityCache.clear();
            entityCache.addAll(level.getEntities((Entity) null, area, entity -> {
                EntityType<?> type = entity.getType();
                ResourceLocation entityTypeName = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                return !(DEFAULT_BLACKLIST.contains(MatchSet.Entry.entity(entityTypeName)) || matchSet.matchEntity(entityTypeName, type));
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
        assert level != null;
        int extraTicks = accelerateMultiplier - 1;
        int randomTick = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        BlockPos.betweenClosedStream(area).forEach(pos -> {
            assert level != null;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) return;
            ResourceLocation blockName = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (DEFAULT_BLACKLIST.contains(MatchSet.Entry.block(blockName))) return;
            if (blacklistMode != matchSet.matchBlock(blockName, state)) {
                if (state.isRandomlyTicking() && level.random.nextInt(randomTick) < Math.min(extraTicks, randomTick)) {
                    state.randomTick((ServerLevel) level, pos, level.random);
                } else {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be == null) return;
                    BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) state.getTicker(level, be.getType());
                    if (be.isRemoved() || ticker == null) return;
                    for (int i = 0; i < extraTicks; ++i) {
                        ticker.tick(level, pos, state, be);
                    }
                }
            }
        });
    }

    private void accelerateEntities() {
        int extraTicks = accelerateMultiplier - 1;
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
        this.accelerateMultiplier = Mth.clamp(accelerateMultiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
        this.consumed = consumptionOf(this.accelerateMultiplier);
        setChanged();
    }

    private static int consumptionOf(int multiplier) {
        return multiplier <= 1024 ? CONSUMPTION[multiplier]
                : (int) (BASE_CONSUMPTION * (multiplier * multiplier + StrictMath.pow(multiplier, 1.8) * StrictMath.log(multiplier) * 1.4426950408889634));
    }

    @Override
    public void setBlacklistMode(boolean b) {
        this.blacklistMode = b;
        markUpdate();
    }

    @Override
    public void addMatchSet(MatchSet.Entry entry) {
        if (matchSet.add(entry)) markUpdate();
    }

    @Override
    public void removeMatchSet(MatchSet.Entry entry) {
        if (matchSet.remove(entry)) markUpdate();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    @NotNull
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.accelerateMultiplier = Mth.clamp(tag.getInt("acc_multiplier"), MIN_MULTIPLIER, MAX_MULTIPLIER);
        this.blacklistMode = tag.getBoolean("blacklist");
        this.consumed = consumptionOf(this.accelerateMultiplier);
        this.matchSet.clear();
        MatchSet.CODEC.parse(NbtOps.INSTANCE, tag.getList("matchset", Tag.TAG_COMPOUND))
                .resultOrPartial(error -> log.warn("Failed to load matchset: {}", error))
                .ifPresent(loaded -> loaded.ordered().forEach(this.matchSet::add));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("acc_multiplier", this.accelerateMultiplier);
        tag.putBoolean("blacklist", this.blacklistMode);
        MatchSet.CODEC.encodeStart(NbtOps.INSTANCE, this.matchSet)
                .resultOrPartial(error -> log.warn("Failed to save matchset: {}", error))
                .ifPresent(nbt -> tag.put("matchset", nbt));
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return Component.translatable("menu.tempusetchaos.accelerator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MenuAccelerator(containerId, playerInventory, this);
    }
}
