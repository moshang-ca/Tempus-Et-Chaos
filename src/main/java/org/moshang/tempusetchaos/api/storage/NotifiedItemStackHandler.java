package org.moshang.tempusetchaos.api.storage;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class NotifiedItemStackHandler extends ItemStackHandler {
    private final IO io;
    @Accessors(fluent = true) @Getter(lazy = true)
    private final IItemHandler guiAccessor = new HandlerGuiAccessor();

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
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (io == IO.INPUT || io == IO.BOTH)
            return super.insertItem(slot, stack, simulate);
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (io == IO.OUTPUT || io == IO.BOTH)
            return super.extractItem(slot, amount, simulate);
        return ItemStack.EMPTY;
    }

    /** Consumes the machine's own inputs: {@link #extractItem} is closed off on an {@link IO#INPUT} handler. */
    public ItemStack forceExtract(int slot, int amount, boolean simulate) {
        return super.extractItem(slot, amount, simulate);
    }

    /** Puts the machine's own results in: {@link #insertItem} is closed off on an {@link IO#OUTPUT} handler. */
    public ItemStack forceInsert(int slot, ItemStack stack, boolean simulate) {
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    protected void onContentsChanged(int slot) {
        onContentsChanged.run();
    }

    public class HandlerGuiAccessor implements IItemHandler, IItemHandlerModifiable {

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            NotifiedItemStackHandler.this.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return NotifiedItemStackHandler.this.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return NotifiedItemStackHandler.this.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return NotifiedItemStackHandler.this.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return NotifiedItemStackHandler.super.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return NotifiedItemStackHandler.this.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return NotifiedItemStackHandler.this.isItemValid(slot, stack);
        }
    }
}
