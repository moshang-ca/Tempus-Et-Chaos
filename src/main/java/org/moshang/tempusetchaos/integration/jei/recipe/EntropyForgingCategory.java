package org.moshang.tempusetchaos.integration.jei.recipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.ItemResult;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.ValueQuantity;
import org.moshang.tempusetchaos.recipe.ProcessingRecipe;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.FluidAmount;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.ItemRequirement;
import org.moshang.tempusetchaos.registry.TECBlocks;
import org.moshang.tempusetchaos.util.TECLocalizations;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * Forging stacks its materials in columns: items above fluids, costs on the left and results on the right, so the
 * value layer has the middle to itself.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EntropyForgingCategory extends AbstractProcessingCategory<ProcessingRecipe.EntropyForging> {
    private static final int WIDTH = 136;
    private static final int HEIGHT = 82;
    private static final int STEP = 20;
    private static final int CELL = 18;
    private static final int OUTPUT_X = 96;
    private static final int TEXT_X = 26;
    private static final int VALUE_OUT_X = 48;
    private static final int VALUE_Y = 16;

    public EntropyForgingCategory(RecipeType<ProcessingRecipe.EntropyForging> recipeType) {
        super(recipeType, Component.translatable(TECLocalizations.JEI_ENTROPY_FORGING),
                new ItemStack(TECBlocks.ENTROPY_FORGE.get()));
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProcessingRecipe.EntropyForging recipe, IFocusGroup focuses) {
        List<ItemRequirement> items = recipe.inputs().items();
        for (int i = 0; i < items.size(); i++) {
            itemInput(builder, 1, 1 + i * STEP, items.get(i));
        }
        List<FluidAmount> fluids = recipe.inputs().fluids();
        for (int i = 0; i < fluids.size(); i++) {
            fluidInput(builder, 1, 1 + (items.size() + i) * STEP, fluids.get(i));
        }

        List<ItemResult> results = recipe.outputs().items();
        for (int i = 0; i < results.size(); i++) {
            itemOutput(builder, OUTPUT_X, 1 + i * STEP, results.get(i));
        }
        List<FluidAmount> fluidResults = recipe.outputs().fluids();
        for (int i = 0; i < fluidResults.size(); i++) {
            fluidOutput(builder, OUTPUT_X, 1 + (results.size() + i) * STEP, fluidResults.get(i));
        }
    }

    @Override
    public void draw(ProcessingRecipe.EntropyForging recipe, IRecipeSlotsView slots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        drawTime(graphics, recipe, TEXT_X, 5);
        for (ValueBox box : valueBoxes(recipe)) {
            drawValue(graphics, box);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ProcessingRecipe.EntropyForging recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        tooltip.addAll(valueTooltip(valueBoxes(recipe), mouseX, mouseY));
    }

    private static List<ValueBox> valueBoxes(ProcessingRecipe.EntropyForging recipe) {
        List<ValueBox> boxes = new ArrayList<>();
        int y = VALUE_Y;
        y = addValue(boxes, recipe.valuesIn().chronon(), ValueKind.CHRONON, TEXT_X, y);
        y = addValue(boxes, recipe.valuesIn().energy(), ValueKind.ENERGY, TEXT_X, y);
        y = VALUE_Y;
        y = addValue(boxes, recipe.valuesOut().chronon(), ValueKind.CHRONON, VALUE_OUT_X, y);
        addValue(boxes, recipe.valuesOut().energy(), ValueKind.ENERGY, VALUE_OUT_X, y);
        return boxes;
    }

    private static int addValue(List<ValueBox> boxes, ValueQuantity value, ValueKind kind, int x, int y) {
        if (value.isEmpty()) return y;
        boxes.add(new ValueBox(x, y, CELL, CELL, kind, value.amount(), value.perTick()));
        return y + STEP;
    }
}
