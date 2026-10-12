package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An element the player fills by dropping an ingredient on it from outside the screen, without any real
 * {@code Slot} behind it.
 */
@ParametersAreNonnullByDefault
public interface GhostSlot {
    boolean accepts(ItemStack stack);

    void accept(ItemStack stack);
}
