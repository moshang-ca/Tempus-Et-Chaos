package org.moshang.tempusetchaos.client.gui;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public interface WindowScreen {
    /** The ghost slots of this screen that take the stack, in screen coordinates. */
    List<WindowManager.GhostTarget> ghostSlots(ItemStack stack);

    @Nullable
    Rect2i windowArea();
}
