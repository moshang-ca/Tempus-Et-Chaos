package org.moshang.tempusetchaos.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.client.gui.AbstractWindowScreen;
import org.moshang.tempusetchaos.client.gui.ContainerWindowScreen;
import org.moshang.tempusetchaos.integration.jei.gui.GhostSlotHandler;
import org.moshang.tempusetchaos.integration.jei.gui.WindowRecipeHandler;
import org.moshang.tempusetchaos.integration.jei.gui.WindowScreenProperties;
import org.moshang.tempusetchaos.integration.jei.recipe.EntropyForgingCategory;
import org.moshang.tempusetchaos.integration.jei.recipe.TimeExtractingCategory;
import org.moshang.tempusetchaos.recipe.ProcessingRecipe;
import org.moshang.tempusetchaos.registry.TECRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@JeiPlugin
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TECJeiPlugin implements IModPlugin {
    private static final RecipeType<ProcessingRecipe.TimeExtracting> TIME_EXTRACTING =
            RecipeType.create(TempusEtChaos.MODID, "time_extracting", ProcessingRecipe.TimeExtracting.class);
    private static final RecipeType<ProcessingRecipe.EntropyForging> ENTROPY_FORGING =
            RecipeType.create(TempusEtChaos.MODID, "entropy_forging", ProcessingRecipe.EntropyForging.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new TimeExtractingCategory(TIME_EXTRACTING),
                new EntropyForgingCategory(ENTROPY_FORGING));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TIME_EXTRACTING, recipes(TECRecipes.TIME_EXTRACTOR_RT.get()));
        registration.addRecipes(ENTROPY_FORGING, recipes(TECRecipes.ENTROPY_FORGE_RT.get()));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(AbstractWindowScreen.class, new WindowScreenProperties<>());
        registration.addGuiScreenHandler(ContainerWindowScreen.class, new WindowScreenProperties<>());

        registration.addGenericGuiContainerHandler(ContainerWindowScreen.class, new WindowRecipeHandler<>());

        registration.addGhostIngredientHandler(AbstractWindowScreen.class, new GhostSlotHandler<>());
        registration.addGhostIngredientHandler(ContainerWindowScreen.class, new GhostSlotHandler<>());
    }

    private static <R extends ProcessingRecipe> List<R> recipes(net.minecraft.world.item.crafting.RecipeType<R> type) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        return level.getRecipeManager().getAllRecipesFor(type).stream().map(RecipeHolder::value).toList();
    }
}
