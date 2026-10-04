package org.moshang.tempusetchaos.integration.jei.gui;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.client.gui.WindowManager;
import org.moshang.tempusetchaos.client.gui.WindowScreen;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns the ghost slots of a window screen into JEI drop targets. The same logic serves every window screen, so it is
 * written against {@link WindowScreen} and registered once per screen base.
 */
@ParametersAreNonnullByDefault
public class GhostSlotHandler<T extends Screen & WindowScreen> implements IGhostIngredientHandler<T> {
    @Override
    @NotNull
    public <I> List<Target<I>> getTargetsTyped(T gui, ITypedIngredient<I> ingredient, boolean doStart) {
        Optional<ItemStack> stack = ingredient.getItemStack();
        if (stack.isEmpty()) return List.of();

        List<Target<I>> targets = new ArrayList<>();
        for (WindowManager.GhostTarget target : gui.ghostSlots(stack.get())) {
            targets.add(new Target<>() {
                @Override
                @NotNull
                public Rect2i getArea() {
                    return target.area();
                }

                @Override
                public void accept(I ingredient) {
                    // only item ingredients reach this list, so the stack the drag was started with is the value
                    target.slot().accept(stack.get());
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
