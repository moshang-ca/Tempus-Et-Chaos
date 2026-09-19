package org.moshang.tempusetchaos.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.api.IWrench;
import org.moshang.tempusetchaos.registry.TECCapabilities;

public class EntropyWrench extends Item {
    public EntropyWrench(Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        IWrench wrench = stack.getCapability(TECCapabilities.WRENCH);
        if (wrench == null) return InteractionResult.PASS;
        Level level = context.getLevel();

        IWrench.WrenchHit hit = new IWrench.WrenchHit(context.getClickedPos(), context.getClickedFace(), context.getClickLocation());
        return wrench.useOnBlock(level, hit, player) ?
                InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }
}
