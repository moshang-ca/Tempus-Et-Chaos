package org.moshang.tempusetchaos.integration.jei.recipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.element.ResourceGaugeElement;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.ItemResult;
import org.moshang.tempusetchaos.recipe.ProcessingRecipe;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.FluidAmount;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.ItemRequirement;
import org.moshang.tempusetchaos.util.TECLocalizations;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * What every processing category shares: the icon, the slot and text helpers, and the drawing of the value layer. A
 * category still owns its layout, because each recipe type arranges its materials differently.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractProcessingCategory<R extends ProcessingRecipe> implements IRecipeCategory<R> {
    protected static final int SLOT = 18;
    protected static final int COLOR = 0xFF404040;

    private final RecipeType<R> recipeType;
    private final Component title;
    private final IDrawable icon;

    protected AbstractProcessingCategory(RecipeType<R> recipeType, Component title, ItemStack icon) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = new ItemIcon(icon);
    }

    @Override
    public RecipeType<R> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public abstract void setRecipe(IRecipeLayoutBuilder builder, R recipe, IFocusGroup focuses);

    protected static void itemInput(IRecipeLayoutBuilder builder, int x, int y, ItemRequirement requirement) {
        builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                .addIngredients(requirement.ingredient())
                .addRichTooltipCallback((view, tooltip) -> {
                    if (requirement.chance() < 1f) {
                        tooltip.add(Component.translatable(TECLocalizations.JEI_CHANCE, requirement.chance() * 100));
                    }
                    tooltip.add(Component.translatable(TECLocalizations.JEI_TICK, requirement.perTick()));
                });
    }

    protected static void itemOutput(IRecipeLayoutBuilder builder, int x, int y, ItemResult result) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                .addItemStack(result.stack())
                .addRichTooltipCallback((view, tooltip) -> {
                    if (result.chance() < 1f) {
                        tooltip.add(Component.translatable(TECLocalizations.JEI_CHANCE, result.chance() * 100));
                    }
                    tooltip.add(Component.translatable(TECLocalizations.JEI_TICK, result.perTick()));
                });
    }

    protected static void fluidInput(IRecipeLayoutBuilder builder, int x, int y, FluidAmount amount) {
        builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                .addIngredient(NeoForgeTypes.FLUID_STACK, amount.stack())
                .addRichTooltipCallback((view, tooltip) -> {
                    if (amount.chance() < 1f) {
                        tooltip.add(Component.translatable(TECLocalizations.JEI_CHANCE, amount.chance() * 100));
                    }
                    tooltip.add(Component.translatable(TECLocalizations.JEI_TICK, amount.perTick()));
                });
    }

    protected static void fluidOutput(IRecipeLayoutBuilder builder, int x, int y, FluidAmount amount) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                .addIngredient(NeoForgeTypes.FLUID_STACK, amount.stack())
                .addRichTooltipCallback((view, tooltip) -> {
                    if (amount.chance() < 1f) {
                        tooltip.add(Component.translatable(TECLocalizations.JEI_CHANCE, amount.chance() * 100));
                    }
                    tooltip.add(Component.translatable(TECLocalizations.JEI_TICK, amount.perTick()));
                });
    }

    protected static void drawValue(GuiGraphics graphics, ValueBox box) {
        if (box.kind() == ValueKind.CHRONON) {
            ResourceGaugeElement.draw(graphics, box.x(), box.y(), box.width(), box.height(),
                    UiTextures.CHANNEL, 1f, UiTextures.sprite("chronon"), -1);
        } else {
            // top-left, so the stack-like amount in the bottom-right stays readable
            graphics.drawString(Minecraft.getInstance().font, box.kind().label, box.x() + 2, box.y() + 1,
                    box.kind().color, false);
        }
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, box.amount, box.x() + box.width() - font.width(box.amount),
                box.y() + box.height() - 9, 0xFFFFFFFF, true);
    }

    /** The tooltip of the value box under the mouse, empty when there is none. */
    protected static List<Component> valueTooltip(List<ValueBox> boxes, double mouseX, double mouseY) {
        for (ValueBox box : boxes) {
            if (mouseX >= box.x() && mouseX < box.x() + box.width()
                    && mouseY >= box.y() && mouseY < box.y() + box.height()) {
                return List.of(Component.translatable(box.kind().unitKey, box.amount()),
                        Component.translatable(TECLocalizations.JEI_TICK, box.perTick()));
            }
        }
        return List.of();
    }

    protected static void drawTime(GuiGraphics graphics, ProcessingRecipe recipe, int x, int y) {
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable(TECLocalizations.JEI_TIME, recipe.processingTime()), x, y, COLOR, false);
    }

    protected enum ValueKind {
        CHRONON("CH", TECLocalizations.JEI_CHRONON, 0xFF10EE10),
        ENERGY("FE", TECLocalizations.JEI_ENERGY, 0xFFD05050);

        private final String label;
        private final String unitKey;
        private final int color;

        ValueKind(String label, String unitKey, int color) {
            this.label = label;
            this.unitKey = unitKey;
            this.color = color;
        }
    }

    protected record ValueBox(int x, int y, int width, int height, ValueKind kind, String amount, boolean perTick) {
        public ValueBox(int x, int y, int width, int height, ValueKind kind, long amount, boolean perTick) {
            this(x, y, width, height, kind,
                    TECLocalizations.formatCompact(amount, false, ""), perTick);
        }
    }

    private record ItemIcon(ItemStack stack) implements IDrawable {
        @Override
        public int getWidth() {
            return 16;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
            graphics.renderItem(stack, xOffset, yOffset);
        }
    }
}
