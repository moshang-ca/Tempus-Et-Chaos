package org.moshang.tempusetchaos.block;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.IWrench;
import org.moshang.tempusetchaos.api.IWrenchable;
import org.moshang.tempusetchaos.api.VesselTier;
import org.moshang.tempusetchaos.blockentity.BEEntropyVessel;
import org.moshang.tempusetchaos.registry.TECItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockEntropyVessel extends Block implements EntityBlock, IWrenchable {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    private static final VoxelShape[] MODULE_SHAPES = {
            Block.box(3, 0, 12, 13, 16, 16),   // SOUTH
            Block.box(0, 0, 3, 4, 16, 13),     // WEST
            Block.box(3, 0, 0, 13, 16, 4),     // NORTH
            Block.box(12, 0, 3, 16, 16, 13)    // EAST
    };

    private static final VoxelShape[] COLLISION_SHAPES = new VoxelShape[16];

    static {
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = SHAPE;
            for (int i = 0; i < 4; i++) {
                if ((mask & (1 << i)) != 0) shape = Shapes.or(shape, MODULE_SHAPES[i]);
            }
            COLLISION_SHAPES[mask] = shape;
        }
    }

    @Getter
    private final VesselTier tier;

    public BlockEntropyVessel(Properties properties, VesselTier tier) {
        super(properties.pushReaction(PushReaction.BLOCK).noOcclusion());
        this.tier = tier;
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
    @NotNull
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @NotNull
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof BEEntropyVessel vessel) {
            return COLLISION_SHAPES[vessel.getCoolingMask()];
        }
        return SHAPE;
    }

    @Override
    public boolean onShiftedInteraction(Level level, BlockState state, IWrench.WrenchHit hitResult, @Nullable Player player) {
        if (hitResult.face().get2DDataValue() != -1) {
            if (level.getBlockEntity(hitResult.pos()) instanceof BEEntropyVessel vessel) {
                int duration = vessel.removeCooling(hitResult.face());
                if (duration > 0) {
                    ItemStack toDrop = new ItemStack(TECItems.ENTROPY_COOLING_MODULE.get());
                    toDrop.set(DataComponents.DAMAGE, toDrop.getMaxDamage() - duration);
                    if (player == null || !player.addItem(toDrop)) {
                        Containers.dropItemStack(level, hitResult.pos().getX(), hitResult.pos().getY(), hitResult.pos().getZ(), toDrop);
                        return true;
                    }
                } else return IWrenchable.super.onShiftedInteraction(level, state, hitResult, player);
            }
        } else return IWrenchable.super.onShiftedInteraction(level, state, hitResult, player);
        return false;
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
