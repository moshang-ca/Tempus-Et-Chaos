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
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.element.ProgressElement;
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
 * Extraction turns its materials into chronon and has no result slots at all, so the page reads as
 * {@code input -> chronon cell} with the numbers beside it.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TimeExtractingCategory extends AbstractProcessingCategory<ProcessingRecipe.TimeExtracting> {
    private static final int WIDTH = 132;
    private static final int HEIGHT = 56;
    private static final int ARROW_X = 22;
    private static final int ARROW_Y = 1;
    private static final int ARROW_WIDTH = 50;
    private static final int ARROW_HEIGHT = 14;
    private static final int PAYOUT_X = 74;
    private static final int PAYOUT_Y = 1;
    private static final int CELL = 18;
    private static final int STEP = 20;
    private static final int TEXT_X = 96;
    private static final int TEXT_Y = 4;

    public TimeExtractingCategory(RecipeType<ProcessingRecipe.TimeExtracting> recipeType) {
        super(recipeType, Component.translatable(TECLocalizations.JEI_TIME_EXTRACTING),
                new ItemStack(TECBlocks.TIME_EXTRACTOR.get()));
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
    public void setRecipe(IRecipeLayoutBuilder builder, ProcessingRecipe.TimeExtracting recipe, IFocusGroup focuses) {
        List<ItemRequirement> items = recipe.inputs().items();
        for (int i = 0; i < items.size(); i++) {
            itemInput(builder, 1 + i * SLOT, 1, items.get(i));
        }
        List<FluidAmount> fluids = recipe.inputs().fluids();
        for (int i = 0; i < fluids.size(); i++) {
            fluidInput(builder, 1 + i * SLOT, 1 + SLOT, fluids.get(i));
        }
    }

    @Override
    public void draw(ProcessingRecipe.TimeExtracting recipe, IRecipeSlotsView slots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        ProgressElement.draw(graphics, ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT, UiTextures.PROGRESS, 9, 54, 36);
        for (ValueBox box : valueBoxes(recipe)) {
            drawValue(graphics, box);
        }
        drawTime(graphics, recipe, TEXT_X, TEXT_Y);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ProcessingRecipe.TimeExtracting recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        tooltip.addAll(valueTooltip(valueBoxes(recipe), mouseX, mouseY));
    }

    private static List<ValueBox> valueBoxes(ProcessingRecipe.TimeExtracting recipe) {
        List<ValueBox> boxes = new ArrayList<>();
        int y = TEXT_Y + 12;
        y = addValue(boxes, recipe.valuesIn().chronon(), ValueKind.CHRONON, TEXT_X, y);
        y = addValue(boxes, recipe.valuesIn().energy(), ValueKind.ENERGY, TEXT_X, y);
        ValueQuantity payout = recipe.valuesOut().chronon();
        if (!payout.isEmpty()) {
            boxes.add(new ValueBox(PAYOUT_X, PAYOUT_Y, CELL, CELL, ValueKind.CHRONON, payout.amount(), payout.perTick()));
        }
        addValue(boxes, recipe.valuesOut().energy(), ValueKind.ENERGY, TEXT_X, y);
        return boxes;
    }

    private static int addValue(List<ValueBox> boxes, ValueQuantity value, ValueKind kind, int x, int y) {
        if (value.isEmpty()) return y;
        boxes.add(new ValueBox(x, y, CELL, CELL, kind, value.amount(), value.perTick()));
        return y + STEP;
    }
}
