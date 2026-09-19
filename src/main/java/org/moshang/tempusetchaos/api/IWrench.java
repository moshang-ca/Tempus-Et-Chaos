package org.moshang.tempusetchaos.api;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.registry.TECCapabilities;

public interface IWrench {
    @CanIgnoreReturnValue
    default boolean useOnBlock(Level level, WrenchHit hit, @Nullable Player player) {
        IWrenchable wrenchable = level.getCapability(TECCapabilities.WRENCHABLE, hit.pos());
        return wrenchable != null && wrenchable.onWrench(level, hit, player != null && player.isShiftKeyDown(), player);
    }

    class WrenchHandler implements IWrench {}

    record WrenchHit(BlockPos pos, Direction face, Vec3 location) { }
}
