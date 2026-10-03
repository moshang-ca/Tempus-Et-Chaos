package org.moshang.tempusetchaos.integration.jei.gui;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.client.gui.AbstractWindowScreen;
import org.moshang.tempusetchaos.client.gui.WindowManager;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class GhostSlotHandler implements IGhostIngredientHandler<AbstractWindowScreen> {
    @Override
    @NotNull
    public <I> List<Target<I>> getTargetsTyped(AbstractWindowScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
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
