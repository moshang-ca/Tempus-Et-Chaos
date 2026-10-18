package org.moshang.tempusetchaos.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.FluidAmount;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.ItemRequirement;
import org.moshang.tempusetchaos.recipe.input.MaterialsInput;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * Counts and moves material amounts across several sources, so that matching a recipe and draining it for real can
 * never disagree. A machine hands in its live slots and tanks, a recipe hands in a read-only view of its input.
 * <p>
 * Chance is not this class's business: whoever asks for a move has already resolved the rolls into concrete amounts.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class MaterialLedger {
    private final List<ItemSource> items;
    private final List<FluidSource> fluids;

    /** A slot a machine can really empty. */
    public interface ItemSource {
        ItemStack peek();

        /** Takes at most {@code amount} items, honouring {@code simulate}. */
        ItemStack extract(int amount, boolean simulate);
    }

    /** A tank a machine can really drain. */
    public interface FluidSource {
        FluidStack peek();

        /** Drains at most {@code amount} mB, honouring {@code simulate}. */
        FluidStack drain(int amount, boolean simulate);
    }

    private MaterialLedger(List<ItemSource> items, List<FluidSource> fluids) {
        this.items = items;
        this.fluids = fluids;
    }

    /** A read-only view of what a recipe was offered; nothing can be taken out of it. */
    public static MaterialLedger of(MaterialsInput input) {
        List<ItemSource> items = new ArrayList<>(input.items().size());
        for (ItemStack stack : input.items()) {
            items.add(new ItemSource() {
                @Override
                public ItemStack peek() {
                    return stack;
                }

                @Override
                public ItemStack extract(int amount, boolean simulate) {
                    return stack.copyWithCount(Math.min(amount, stack.getCount()));
                }
            });
        }
        List<FluidSource> fluids = new ArrayList<>(input.fluids().size());
        for (FluidStack stack : input.fluids()) {
            fluids.add(new FluidSource() {
                @Override
                public FluidStack peek() {
                    return stack;
                }

                @Override
                public FluidStack drain(int amount, boolean simulate) {
                    return stack.copyWithAmount(Math.min(amount, stack.getAmount()));
                }
            });
        }
        return new MaterialLedger(items, fluids);
    }

    public static MaterialLedger of(List<ItemSource> items, List<FluidSource> fluids) {
        return new MaterialLedger(List.copyOf(items), List.copyOf(fluids));
    }

    public boolean canTake(RecipeMaterials materials, int factor) {
        return move(materials, factor, true);
    }

    public boolean take(RecipeMaterials materials, int factor) {
        return move(materials, factor, false);
    }

    /** Everything is checked before anything moves, so a failed take leaves every source untouched. */
    private boolean move(RecipeMaterials materials, int factor, boolean simulate) {
        if (!fits(materials, factor)) return false;
        if (simulate) return true;
        for (ItemRequirement requirement : materials.items()) {
            takeItems(requirement, factor, false);
        }
        for (FluidAmount amount : materials.fluids()) {
            drainFluids(amount, factor, false);
        }
        return true;
    }

    private boolean fits(RecipeMaterials materials, int factor) {
        for (ItemRequirement requirement : materials.items()) {
            if (takeItems(requirement, factor, true) > 0) return false;
        }
        for (FluidAmount amount : materials.fluids()) {
            if (drainFluids(amount, factor, true) > 0) return false;
        }
        return true;
    }

    /** Takes one requirement across as many sources as it needs; returns what is still missing. */
    private int takeItems(ItemRequirement requirement, int factor, boolean simulate) {
        int needed = requirement.count() * factor;
        for (ItemSource source : items) {
            if (needed <= 0) break;
            ItemStack stack = source.peek();
            if (stack.isEmpty() || !requirement.ingredient().test(stack)) continue;
            needed -= source.extract(Math.min(needed, stack.getCount()), simulate).getCount();
        }
        return Math.max(needed, 0);
    }

    /** Drains one amount across as many sources as it needs; returns the still missing mB. */
    private int drainFluids(FluidAmount amount, int factor, boolean simulate) {
        int needed = amount.stack().getAmount() * factor;
        for (FluidSource source : fluids) {
            if (needed <= 0) break;
            FluidStack stack = source.peek();
            if (stack.isEmpty() || !FluidStack.isSameFluidSameComponents(stack, amount.stack())) continue;
            needed -= source.drain(Math.min(needed, stack.getAmount()), simulate).getAmount();
        }
        return Math.max(needed, 0);
    }
}
