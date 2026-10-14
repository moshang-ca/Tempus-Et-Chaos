package org.moshang.tempusetchaos.recipe;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.moshang.tempusetchaos.registry.TECRecipes;
import org.moshang.tempusetchaos.util.TECConstants;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@Builder
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TimeExtractorRecipe implements Recipe<SingleRecipeInput>, ITimeConsumable {
    private final ItemRequirement inputItem;
    @Builder.Default
    private final int processingTime = TECConstants.DEFAULT_PROCESS_TIME;
    private final int outputChronon;
    @Builder.Default
    private final boolean perTick = false;

    public TimeExtractorRecipe(ItemRequirement inputItem, int processingTime, int outputChronon, boolean perTick) {
        if (outputChronon <= 0) throw new IllegalArgumentException("outputChronon must be > 0");
        this.inputItem = inputItem;
        this.processingTime = Math.max(1, processingTime);
        this.outputChronon = outputChronon;
        this.perTick = perTick;
    }

    /** Chronon paid out on every tick the recipe runs. */
    public int chrononPerTick() {
        return perTick ? outputChronon : 0;
    }

    /** Chronon paid out in one go when the recipe finishes. */
    public int chrononOnFinish() {
        return perTick ? 0 : outputChronon;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        ItemStack stack = input.getItem(0);
        return stack.getCount() >= inputItem.count() && inputItem.ingredient().test(stack);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(inputItem.ingredient());
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TECRecipes.TIME_EXTRACTOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TECRecipes.TIME_EXTRACTOR_RT.get();
    }
}
