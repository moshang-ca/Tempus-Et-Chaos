package org.moshang.tempusetchaos.integration.jei.gui;

import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import org.moshang.tempusetchaos.client.gui.ContainerWindowScreen;
import org.moshang.tempusetchaos.client.gui.WindowManager;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

/**
 * Lets JEI look up the recipe of a slot of a window screen. JEI reads the hovered slot of the screen itself, which this
 * screen never has: the slots belong to the windows, so the windows are asked instead, stacking and animations
 * included. The same logic serves every window screen, so it is written against {@link ContainerWindowScreen} and
 * registered once per screen type.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class WindowRecipeHandler<T extends ContainerWindowScreen<?>> implements IGuiContainerHandler<T> {
    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory builder, T containerScreen, double mouseX, double mouseY) {
        WindowManager.SlotTarget target = containerScreen.slotAt(mouseX, mouseY);
        if (target == null) return Optional.empty();
        ItemStack stack = target.stack();
        if (stack.isEmpty()) return Optional.empty();
        return builder.createBuilder(stack).buildWithArea(target.area());
    }
}
