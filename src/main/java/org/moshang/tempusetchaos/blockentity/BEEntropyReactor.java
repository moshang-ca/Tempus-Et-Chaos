package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.api.EntropyModifiers;
import org.moshang.tempusetchaos.api.storage.IO;
import org.moshang.tempusetchaos.api.storage.NotifiedItemStackHandler;
import org.moshang.tempusetchaos.data.EntropyCompensation;
import org.moshang.tempusetchaos.data.EntropyWorldData;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECFluids;
import org.moshang.tempusetchaos.util.TECConstants;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BEEntropyReactor extends BaseChrononNodeBlockEntity {
    private static final int BASE_CONSUMPTION = 10;
    private static final int BASE_COMPENSATION_INTERVAL = 100;
    private static final int UPGRADE_SLOTS = 1;
    private static final int OUTPUT_SLOTS = 2;

    @Getter
    private final FluidTank fluidHandler = new FluidTank(51200, stack -> stack.is(TECFluids.GAS_ENTROPY_TYPE.get()));
    /** Upgrade items: reserved for the upgrade system */
    @Getter
    private final NotifiedItemStackHandler upgradeHandler = new NotifiedItemStackHandler(UPGRADE_SLOTS, IO.BOTH)
            .setOnContentsChanged(() -> { setChanged(); interval = compensationInterval(); });
    @Getter
    private final NotifiedItemStackHandler outputHandler = new NotifiedItemStackHandler(OUTPUT_SLOTS, IO.OUTPUT)
            .setOnContentsChanged(this::setChanged);
    @Setter
    private EntropyModifiers modifiers = EntropyModifiers.NONE;

    private final ChunkPos inChunk;
    private EntropyWorldData entropyData;
    private int compensationTimer;
    private long interval = BASE_COMPENSATION_INTERVAL;

    public BEEntropyReactor(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.ENTROPY_REACTOR_BE.get(), pos, blockState, 5000);
        inChunk = new ChunkPos(pos);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) entropyData = EntropyWorldData.get(serverLevel);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (entropyData == null && level instanceof ServerLevel serverLevel) {
            entropyData = EntropyWorldData.get(serverLevel);
        }
        if (entropyData == null) return;
        if (innerNetwork == null || innerNetwork.extractChronon(consumed(), true) < consumed()) {
            leak();
            return;
        }
        float concentration = entropyData.getConcentration(inChunk);
        int gas = gasPerTick(concentration);
        if (gas <= 0) return;
        FluidStack output = new FluidStack(TECFluids.GAS_ENTROPY_SOURCE.get(), gas);
        if (fluidHandler.fill(output, IFluidHandler.FluidAction.SIMULATE) < gas) return;
        innerNetwork.extractChronon(consumed(), false);
        fluidHandler.fill(output, IFluidHandler.FluidAction.EXECUTE);
        compensation(concentration);
    }

    private int consumed() {
        return Math.max(0, Math.round(BASE_CONSUMPTION * modifiers.chrononCost()));
    }

    private int gasPerTick(float concentration) {
        int base = concentration < 3f
                ? Math.max((int) concentration * 10, 15)
                : Math.max((int) concentration * 15, 20);
        return Math.max(0, Math.round(base * modifiers.gasRate()));
    }

    private int compensationInterval() {
        float rate = modifiers.compensationRate();
        if (rate <= 0f) return Integer.MAX_VALUE;
        interval = Math.round(BASE_COMPENSATION_INTERVAL / (double) rate);
        return Math.clamp(interval, 1, Integer.MAX_VALUE);
    }

    /** Every completed interval pays the rewards of the tier the chunk's concentration is in. */
    private void compensation(float concentration) {
        if (interval == Integer.MAX_VALUE) return;
        if (++compensationTimer < interval) return;
        compensationTimer = 0;
        EntropyCompensation.Tier tier = EntropyCompensation.tierFor(concentration);
        if (tier == null) return;
        for (EntropyCompensation.Output output : tier.outputs()) {
            if (output.chance() < 1f && TempusEtChaos.RANDOM.nextFloat() >= output.chance()) continue;
            ItemStack remaining = output.stack().copy();
            for (int slot = 0; slot < outputHandler.getSlots() && !remaining.isEmpty(); slot++) {
                remaining = outputHandler.forceInsert(slot, remaining, false);
            }
            if (!remaining.isEmpty() && level != null) {
                Containers.dropItemStack(level, getBlockPos().getX() + .5, getBlockPos().getY() + .5,
                        getBlockPos().getZ() + .5, remaining);
            }
        }
    }

    /** Without chronon the reactor lets its tank bleed into the chunk: stored entropy becomes concentration. */
    private void leak() {
        FluidStack stored = fluidHandler.drain(fluidHandler.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        if (stored.isEmpty()) return;
        entropyData.addConcentration(inChunk,
                Mth.clamp(stored.getAmount() * TECConstants.ENTROPY_GAS_CONCENTRATION, 0f, 20f));
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    public int getConsumed() {
        return consumed();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidHandler.readFromNBT(registries, tag);
        if (tag.contains("upgrades")) upgradeHandler.deserializeNBT(registries, tag.getCompound("upgrades"));
        if (tag.contains("outputs")) outputHandler.deserializeNBT(registries, tag.getCompound("outputs"));
        compensationTimer = tag.getInt("compensation_timer");
        if (level instanceof ServerLevel serverLevel) entropyData = EntropyWorldData.get(serverLevel);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluidHandler.writeToNBT(registries, tag);
        tag.put("upgrades", upgradeHandler.serializeNBT(registries));
        tag.put("outputs", outputHandler.serializeNBT(registries));
        tag.putInt("compensation_timer", compensationTimer);
    }
}
