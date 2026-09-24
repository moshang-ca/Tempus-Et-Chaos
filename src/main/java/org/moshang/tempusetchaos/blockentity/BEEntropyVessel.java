package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.VesselTier;
import org.moshang.tempusetchaos.block.BlockEntropyVessel;
import org.moshang.tempusetchaos.data.EntropyWorldData;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECUtilities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BEEntropyVessel extends BlockEntity {
    public static final int SCALE = 10000;
    public static final int CRACK_THRESHOLD = 7000;
    private static final int BASE_DAMAGE = 100;
    public static final float CONCENTRATION_PER_MB = 1f / 4096f;
    private static final int COOLING_PER_TUBE = 1500;
    private static final int COOLING_MAX_CUT = 6000;
    private static final int SYNC_INTERVAL_SECONDS = 5;

    private static final String PRESSURE_KEY = "pressure";
    private static final String INTEGRITY_KEY = "integrity";
    private static final String COOLING_MODULES_KEY = "cooling_modules";
    private static final String DAMAGED_KEY = "damaged";

    @Getter
    private final VesselTier tier;
    @Getter
    private final FluidTank fluidHandler;

    @Getter
    private int integrity = SCALE;
    @Getter
    private int pressure = 0;
    private int[] coolingModules = {0, 0, 0, 0};
    @Getter
    private boolean damaged = false;

    private int tickCounter = 0;
    private int syncCooldown = 0;
    private boolean syncPending = false;

    public BEEntropyVessel(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.ENTROPY_VESSEL_BE.get(), pos, blockState);
        this.tier = blockState.getValue(BlockEntropyVessel.TIER);
        this.fluidHandler = new FluidTank(tier.capacity());
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (++tickCounter < 20) return;
        tickCounter = 0;
        secondTick();
    }

    private void secondTick() {
        tickSync();
        if (damaged) return;

        boolean changed = false;
        long stored = entropyAmount();

        if (stored > 0) {
            long increment = (long) tier.increasement() * coolingFactorScaled()
                    / (long) SCALE;
            pressure = (int) Math.min(SCALE, pressure + increment);
            for (int i = 0; i < 4; ++i) {
                if (coolingModules[i] > 0)
                    coolingModules[i]--;
            }
            changed = true;
        } else if (pressure > 0) {
            pressure = 0;
            changed = true;
        }

        if (pressure > CRACK_THRESHOLD) {
            integrity -= (int) ((long) (pressure - CRACK_THRESHOLD) * BASE_DAMAGE * SCALE
                    / (SCALE - CRACK_THRESHOLD) / tier.durability());
            if (integrity < 0) integrity = 0;
            crackParticles();
        }

        if (integrity <= 0) {
            containFailure();
            return;
        }
        if (changed) markChanged(false);
    }

    private void markChanged(boolean immediate) {
        setChanged();
        if (immediate) {
            syncPending = false;
            syncCooldown = SYNC_INTERVAL_SECONDS;
            sendBlockUpdate();
        } else if (syncCooldown <= 0) {
            syncCooldown = SYNC_INTERVAL_SECONDS;
            sendBlockUpdate();
        } else {
            syncPending = true;
        }
    }

    private void tickSync() {
        if (syncCooldown > 0) syncCooldown--;
        if (syncPending && syncCooldown <= 0) {
            syncPending = false;
            syncCooldown = SYNC_INTERVAL_SECONDS;
            sendBlockUpdate();
        }
    }

    private void containFailure() {
        damaged = true;
        integrity = 0;
        long stored = entropyAmount();
        if (stored > 0 && level instanceof ServerLevel serverLevel) {
            EntropyWorldData.get(serverLevel)
                    .addConcentration(new ChunkPos(getBlockPos()), stored * CONCENTRATION_PER_MB);
        }
        fluidHandler.drain(fluidHandler.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        pressure = 0;
        markChanged(true);
    }

    private void crackParticles() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.random.nextInt(4) != 0) return;
        BlockPos pos = getBlockPos();
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                4, 0.4, 0.4, 0.4, 0.02);
    }

    public void repair() {
        damaged = false;
        integrity = SCALE;
        pressure = 0;
        markChanged(true);
    }

    public boolean installCooling(Direction dir, int duration) {
        int offset = dir.get2DDataValue();
        if (offset == -1) return false;
        if (coolingModules[offset] != 0) return false;
        coolingModules[offset] = duration;
        markChanged(true);
        return true;
    }

    /**
    * @return the duration of cooling module.
    * */
    public int removeCooling(Direction dir) {
        int duration = removeCoolingWithoutUpdate(dir);
        if (duration > 0) markChanged(true);
        return duration;
    }

    public int removeCoolingWithoutUpdate(Direction dir) {
        int offset = dir.get2DDataValue();
        if (offset == -1 || coolingModules[offset] == 0) return -1;
        int duration = coolingModules[offset];
        coolingModules[offset] = 0;
        return duration;
    }

    public int coolingCount() {
        return (coolingModules[0] > 0 ? 1 : 0)
                + (coolingModules[1] > 0 ? 1 : 0)
                + (coolingModules[2] > 0 ? 1 : 0)
                + (coolingModules[3] > 0 ? 1 : 0);
    }

    public int getCoolingDuration(int dir) {
        return coolingModules[dir];
    }

    public int coolingFactorScaled() {
        return SCALE - Math.min(COOLING_MAX_CUT, coolingCount() * COOLING_PER_TUBE);
    }

    public long entropyAmount() {
        FluidStack stack = fluidHandler.getFluid();
        return stack.is(TECUtilities.GAS_ENTROPY_TYPE.get()) ? stack.getAmount() : 0L;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluidHandler.writeToNBT(registries, tag);
        tag.putInt(PRESSURE_KEY, pressure);
        tag.putInt(INTEGRITY_KEY, integrity);
        tag.putIntArray(COOLING_MODULES_KEY, coolingModules);
        tag.putBoolean(DAMAGED_KEY, damaged);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidHandler.readFromNBT(registries, tag);
        pressure = tag.getInt(PRESSURE_KEY);
        integrity = tag.contains(INTEGRITY_KEY) ? tag.getInt(INTEGRITY_KEY) : SCALE;
        int[] loaded = tag.getIntArray(COOLING_MODULES_KEY);
        if (loaded.length == coolingModules.length) {
            coolingModules = loaded;
        }
        damaged = tag.getBoolean(DAMAGED_KEY);
    }

    @Override
    @NotNull
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sendBlockUpdate() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public static float normalize(int value, int standard) {
        return (float) value / standard;
    }
}
