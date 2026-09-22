package org.moshang.tempusetchaos.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.VesselTier;
import org.moshang.tempusetchaos.blockentity.BEEntropyVessel;
import org.moshang.tempusetchaos.registry.TECItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockEntropyVessel extends Block implements EntityBlock {
    public static final EnumProperty<VesselTier> TIER = EnumProperty.create("tier", VesselTier.class);

//    public BlockEntropyVessel(Properties properties) {
//        this(properties, VesselTier.IRON);
//    }

    public BlockEntropyVessel(Properties properties, VesselTier tier) {
        super(properties.pushReaction(PushReaction.BLOCK));
        registerDefaultState(getStateDefinition().any().setValue(TIER, tier));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BEEntropyVessel vessel) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    int duration = vessel.removeCoolingWithoutUpdate(direction);
                    if (duration > 0) {
                        ItemStack coolingModule = new ItemStack(TECItems.ENTROPY_COOLING_MODULE.get());
                        coolingModule.set(DataComponents.DAMAGE, coolingModule.getMaxDamage() - duration);
                        popResource(level, pos, coolingModule);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BEEntropyVessel(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof BEEntropyVessel vessel) {
                vessel.serverTick();
            }
        };
    }
}
