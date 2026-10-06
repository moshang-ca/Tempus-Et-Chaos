package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A slot frame the player fills by dropping an ingredient on it from outside the screen. Nothing is stored
 * here: the value lives wherever the window keeps it, the supplier reads it back and the setter writes it.
 */
@ParametersAreNonnullByDefault
public class GhostSlotElement extends SlotElement implements GhostSlot {
    private final Predicate<ItemStack> filter;
    private final Consumer<ItemStack> setter;

    public GhostSlotElement(int x, int y, Supplier<ItemStack> stack, Consumer<ItemStack> setter) {
        this(x, y, stack, s -> true, setter);
    }

    public GhostSlotElement(int x, int y, Supplier<ItemStack> stack, Predicate<ItemStack> filter, Consumer<ItemStack> setter) {
        super(x, y, stack, List::of);
        this.filter = filter;
        this.setter = setter;
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return filter.test(stack);
    }

    @Override
    public void accept(ItemStack stack) {
        setter.accept(stack);
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) accept(ItemStack.EMPTY);
        else if (accepts(carried)) accept(carried.copy());
        return true;
    }

    @Override
    public List<Component> getTooltip() {
        ItemStack item = stack.get();
        return item.isEmpty() ? List.of() : List.of(item.getHoverName());
    }
}
