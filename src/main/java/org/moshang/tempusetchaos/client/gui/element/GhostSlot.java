package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An element the player fills by dropping an ingredient on it from outside the screen, without any real
 * {@code Slot} behind it. Whoever picks the element up asks {@link #accepts} first and then hands the stack
 * to {@link #accept}, which is where the window decides what the new value means for its own data.
 */
@ParametersAreNonnullByDefault
public interface GhostSlot {
    boolean accepts(ItemStack stack);

    void accept(ItemStack stack);
}
