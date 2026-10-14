package org.moshang.tempusetchaos.api;

import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.storage.NotifiedItemStackHandler;
import org.moshang.tempusetchaos.recipe.ITimeConsumable;
import org.moshang.tempusetchaos.recipe.ItemRequirement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class RecipeableChrononNodeBE<I extends RecipeInput, R extends Recipe<I> & ITimeConsumable> extends BaseChrononNodeBlockEntity {
    protected final RecipeType<R> recipeType;
    protected int progress = 0;
    protected int retryCooldown = 0;
    protected int saveCooldown = 0;
    protected boolean recipeDirty = true;
    @Getter
    protected boolean recipeRunning = false;
    @Getter
    protected R recipe = null;
    @Nullable
    private ResourceLocation recipeId = null;

    public RecipeableChrononNodeBE(BlockEntityType<?> type, BlockPos pos, BlockState blockState,
                                   int chrononCapacity, RecipeType<R> recipeType) {
        super(type, pos, blockState, chrononCapacity);
        this.recipeType = recipeType;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (recipe == null && !startRecipe()) {
            recipeRunning = false;
            return;
        }
        recipeRunning = canRun(recipe) && advance(recipe);
        if (++saveCooldown >= 20) {
            saveCooldown = 0;
            setChanged();
        }
    }

    /** A blocked tick leaves no trace: progress only moves once the tick's work landed. */
    private boolean advance(R recipe) {
        int next = progress + 1;
        boolean finishing = next >= recipe.getProcessingTime();
        if ((recipe.isPerTick() || finishing) && !produce(recipe, finishing)) return false;
        progress = next;
        if (finishing) endRecipe();
        return true;
    }

    /** Looks for a recipe and pays for it upfront, so a non-null recipe means its inputs are already consumed. */
    private boolean startRecipe() {
        if (recipeId != null) {
            recipe = findById(recipeId);
            if (recipe != null) return true;
            recipeId = null;
        }
        if (recipeDirty) {
            recipeDirty = false;
        } else if (--retryCooldown > 0) {
            return false;
        }
        assert level != null;
        RecipeHolder<R> found = null;
        for (I input : buildInputs()) {
            found = level.getRecipeManager().getRecipeFor(recipeType, input, level).orElse(null);
            if (found != null) break;
        }
        retryCooldown = 20;
        if (found == null) return false;
        R value = found.value();
        if (!canRun(value) || !onRecipeStart(value)) return false;
        recipe = value;
        recipeId = found.id();
        progress = 0;
        setChanged();
        return true;
    }

    private void endRecipe() {
        recipe = null;
        recipeId = null;
        progress = 0;
        setChanged();
    }

    @Nullable
    private R findById(ResourceLocation id) {
        assert level != null;
        for (RecipeHolder<R> holder : level.getRecipeManager().getAllRecipesFor(recipeType)) {
            if (holder.id().equals(id)) return holder.value();
        }
        return null;
    }

    /** Inputs to try, in order, when looking for a recipe. */
    protected abstract List<I> buildInputs();

    /** Consumes everything the recipe costs. Returning {@code false} must leave the machine untouched. */
    protected boolean onRecipeStart(R recipe) {
        return true;
    }

    /** Work of one paying tick: emit what the tick yields, or change nothing and return {@code false}. */
    protected abstract boolean produce(R recipe, boolean finishing);

    /** Extra condition for processing, checked every tick. */
    protected boolean canRun(R recipe) {
        return true;
    }

    /** Takes every requirement out of the first matching slots, all or nothing. */
    protected static boolean consume(List<ItemRequirement> requirements, NotifiedItemStackHandler handler) {
        for (ItemRequirement requirement : requirements) {
            int needed = requirement.count();
            for (int slot = 0; slot < handler.getSlots() && needed > 0; slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!requirement.ingredient().test(stack)) continue;
                needed -= handler.forceExtract(slot, Math.min(needed, stack.getCount()), true).getCount();
            }
            if (needed > 0) return false;
        }
        for (ItemRequirement requirement : requirements) {
            int needed = requirement.count();
            for (int slot = 0; slot < handler.getSlots() && needed > 0; slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!requirement.ingredient().test(stack)) continue;
                needed -= handler.forceExtract(slot, Math.min(needed, stack.getCount()), false).getCount();
            }
        }
        return true;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("progress");
        recipeDirty = true;
        if (tag.contains("recipe")) recipeId = ResourceLocation.parse(tag.getString("recipe"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("progress", progress);
        if (recipeId != null) tag.putString("recipe", recipeId.toString());
    }
}
