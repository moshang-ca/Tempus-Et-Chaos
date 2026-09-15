package org.moshang.tempusetchaos.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.data.EntropyWorldData;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECBlocks;
import org.moshang.tempusetchaos.registry.TECItems;
import org.moshang.tempusetchaos.registry.TECUtilities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BEEntropyReactor extends BaseChrononNodeBlockEntity {
    private final FluidTank fluidHandler = new FluidTank(50000, stack -> stack.is(TECUtilities.GAS_ENTROPY_TYPE.get()));
    private final IItemHandler itemHandler = new ItemStackHandler(3);       // Maybe we should not allow input slot to be extracted?
    private final int baseConsumption = 10;     // 10 ch/tick
    private final ChunkPos inChunk;

    private EntropyWorldData entropyData = null;

    public BEEntropyReactor(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.ENTROPY_REACTOR_BE.get(), pos, blockState, 5000);
        inChunk = new ChunkPos(pos);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl)
            entropyData = EntropyWorldData.get(sl);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (entropyData == null)
            entropyData = EntropyWorldData.get((ServerLevel) level);
        float concentration = entropyData.getConcentration(inChunk);
        if (concentration < 3f) {
            fluidHandler.fill(new FluidStack(TECUtilities.GAS_ENTROPY_SOURCE.get(), Math.max((int) concentration * 100, 10)), IFluidHandler.FluidAction.EXECUTE);
        } else {
            fluidHandler.fill(new FluidStack(TECUtilities.GAS_ENTROPY_SOURCE.get(), Math.max((int) concentration * 150, 15)), IFluidHandler.FluidAction.EXECUTE);
            if (concentration > 10f) {
                itemHandler.insertItem(1, new ItemStack(TECItems.ENTROPY_CRYSTAL.get(), (int) (concentration / 20f) + 1), false);
                if (concentration > 75f) {
                    //noinspection DataFlowIssue
                    itemHandler.insertItem(2, new ItemStack(TECItems.getBlockItem(TECBlocks.ENTROPY_CRYSTAL_BLOCK.getRegisteredName()), 1), false);
                }
            }
        }
        long consumed = 0;
        if (innerNetwork != null)
            consumed = innerNetwork.extractChronon(baseConsumption, false);
        if (innerNetwork == null || consumed != baseConsumption) {
            FluidStack entropy = fluidHandler.drain(9999999, IFluidHandler.FluidAction.EXECUTE);
            entropyData.addConcentration(inChunk, Mth.clamp(entropy.getAmount() / 1250f, 0, 20));
        }
        System.out.println(fluidHandler.getFluid());
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    public int getConsumed() {
        return baseConsumption;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidHandler.readFromNBT(registries, tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        System.out.println(fluidHandler.writeToNBT(registries, tag));
    }
}
