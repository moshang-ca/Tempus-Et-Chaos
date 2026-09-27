package org.moshang.tempusetchaos.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.moshang.tempusetchaos.registry.TECRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EntropyForgeRecipe implements Recipe<UniversalRecipeInput> {
    public static final int DEFAULT_TIME = 200;

    @Getter
    private final List<ItemRequirement> itemInputs;
    @Getter
    private final List<FluidStack> fluidInputs;
    @Getter
    private final int chrononCost;
    @Getter
    private final int energyCost;
    @Getter
    private final int processingTime;
    @Getter
    private final ItemStack result;

    public EntropyForgeRecipe(List<ItemRequirement> itemInputs, List<FluidStack> fluidInputs,
                              int chrononCost, int energyCost, int processingTime, ItemStack result) {
        if (itemInputs.isEmpty() || itemInputs.size() > 3) {
            throw new IllegalArgumentException("item inputs must be 1..3, got " + itemInputs.size());
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("result must not be empty");
        }
        for (FluidStack fluid : fluidInputs) {
            if (fluid.isEmpty()) throw new IllegalArgumentException("fluid inputs must not be empty");
        }
        this.itemInputs = List.copyOf(itemInputs);
        this.fluidInputs = fluidInputs.stream().map(FluidStack::copy).toList();
        this.chrononCost = Math.max(0, chrononCost);
        this.energyCost = Math.max(0, energyCost);
        this.processingTime = Math.max(1, processingTime);
        this.result = result.copy();
    }

    @Override
    public boolean matches(UniversalRecipeInput input, Level level) {
        return itemsMatch(input) && fluidsMatch(input) && energyMatch(input);
    }

    private boolean itemsMatch(UniversalRecipeInput input) {
        int[] remaining = new int[input.size()];
        for (int i = 0; i < remaining.length; i++) {
            remaining[i] = input.getItem(i).getCount();
        }
        for (ItemRequirement requirement : itemInputs) {
            int needed = requirement.count();
            for (int i = 0; i < remaining.length && needed > 0; i++) {
                ItemStack stack = input.getItem(i);
                if (remaining[i] <= 0 || stack.isEmpty() || !requirement.ingredient().test(stack)) continue;
                int take = Math.min(needed, remaining[i]);
                remaining[i] -= take;
                needed -= take;
            }
            if (needed > 0) return false;
        }
        return true;
    }

    private boolean fluidsMatch(UniversalRecipeInput input) {
        int[] remaining = new int[input.fluidInputs().size()];
        for (int i = 0; i < remaining.length; i++) {
            remaining[i] = input.getFluidInput(i).getAmount();
        }
        for (FluidStack requirement : fluidInputs) {
            int needed = requirement.getAmount();
            for (int i = 0; i < remaining.length && needed > 0; i++) {
                FluidStack stack = input.getFluidInput(i);
                if (remaining[i] <= 0 || stack.isEmpty() || !requirement.is(stack.getFluid())) continue;
                int take = Math.min(needed, remaining[i]);
                remaining[i] -= take;
                needed -= take;
            }
            if (needed > 0) return false;
        }
        return true;
    }

    private boolean energyMatch(UniversalRecipeInput input) {
        // 两种能量都不是必需项：cost 为 0 就表示不要求。
        return input.chronon() >= chrononCost && input.energy() >= energyCost;
    }

    @Override
    public ItemStack assemble(UniversalRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (ItemRequirement requirement : itemInputs) {
            ingredients.add(requirement.ingredient());
        }
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TECRecipes.ENTROPY_FORGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TECRecipes.ENTROPY_FORGE_RT.get();
    }

    public record ItemRequirement(Ingredient ingredient, int count) {
        public static final Codec<ItemRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(ItemRequirement::ingredient),
                Codec.INT.optionalFieldOf("count", 1).forGetter(ItemRequirement::count)
        ).apply(instance, ItemRequirement::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ItemRequirement> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ItemRequirement::ingredient,
                ByteBufCodecs.VAR_INT, ItemRequirement::count,
                ItemRequirement::new);

        public ItemRequirement {
            if (count <= 0) throw new IllegalArgumentException("count must be > 0");
        }
    }
}
