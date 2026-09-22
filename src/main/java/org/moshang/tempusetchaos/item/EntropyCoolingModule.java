package org.moshang.tempusetchaos.item;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.blockentity.BEEntropyVessel;

public class EntropyCoolingModule extends Item {
    public EntropyCoolingModule(Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Direction face = context.getClickedFace();

        if (face.get2DDataValue() == -1) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof BEEntropyVessel vessel)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        int duration = stack.getOrDefault(DataComponents.MAX_DAMAGE, 0)
                - stack.getOrDefault(DataComponents.DAMAGE, 0);
        if (!vessel.installCooling(face, duration)) return InteractionResult.FAIL;

        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
