package org.moshang.tempusetchaos.api.storage;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class NotifiedItemStackHandler extends ItemStackHandler {
    private final IO io;

    @Accessors(chain = true) @Setter
    private Runnable onContentsChanged = () -> {};

    public NotifiedItemStackHandler(int size, IO io) {
        super(size);
        this.io = io;
    }

    public NotifiedItemStackHandler(int size) {
        super(size);
        this.io = IO.BOTH;
    }

    public NotifiedItemStackHandler(NonNullList<ItemStack> stacks, IO io) {
        super(stacks);
        this.io = io;
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (io == IO.INPUT || io == IO.BOTH)
            return super.insertItem(slot, stack, simulate);
        return stack;
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (io == IO.OUTPUT || io == IO.BOTH)
            return super.extractItem(slot, amount, simulate);
        return ItemStack.EMPTY;
    }

    /** Consumes the machine's own inputs: {@link #extractItem} is closed off on an {@link IO#INPUT} handler. */
    public ItemStack forceExtract(int slot, int amount, boolean simulate) {
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    protected void onContentsChanged(int slot) {
        onContentsChanged.run();
    }
}
