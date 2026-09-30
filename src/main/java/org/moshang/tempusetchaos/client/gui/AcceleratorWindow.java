package org.moshang.tempusetchaos.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.client.gui.element.InputFieldElement;
import org.moshang.tempusetchaos.client.gui.element.ResourceGaugeElement;
import org.moshang.tempusetchaos.client.gui.element.SlotElement;
import org.moshang.tempusetchaos.client.gui.element.TextElement;
import org.moshang.tempusetchaos.client.gui.element.TextureButtonElement;
import org.moshang.tempusetchaos.client.gui.element.UiTextures;
import org.moshang.tempusetchaos.menu.MenuAccelerator;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class AcceleratorWindow extends AbstractContainerWindow<MenuAccelerator> {
    private static final int UPGRADE_X = 128;
    private static final int UPGRADE_Y = 17;
    private static final int UPGRADE_SLOTS = 4;

    private final Component title;

    public AcceleratorWindow(MenuAccelerator menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.title = title;

        elements.add(new ResourceGaugeElement(8, 17, 10, 54, UiTextures.CHRONON_CHANNEL, this::chrononRatio, 0xC02E7BD6, ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "block/chronon"), this::chrononTooltip));
        elements.add(new InputFieldElement(22, 17, () -> "x" + menu.getMultiplier(), this::stepMultiplier));
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            elements.add(new SlotElement(UPGRADE_X + i % 2 * 18, UPGRADE_Y + i / 2 * 18, () -> ItemStack.EMPTY, this::upgradeTooltip));
        }
        elements.add(new TextElement(22, 74, 100, 9, () -> Component.literal(menu.getConsumed() + " CH/T"), 0x404040));
        elements.add(new TextureButtonElement(108, 58, () -> Component.literal("Expand"), this::expandBlacklist));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(UiTextures.BASE_INVENTORY, leftPos(), topPos(), imageWidth(), imageHeight(),
                0f, 0f, imageWidth(), imageHeight(), imageWidth(), imageHeight());
        // the player inventory is baked into the background, only the blacklist slots need a frame
        for (int i = 0; i < MenuAccelerator.BLACKLIST_SIZE; i++) {
            Slot slot = menu.slots.get(i);
            graphics.blit(UiTextures.SLOT, leftPos() + slot.x - 1, topPos() + slot.y - 1, 18, 18, 0f, 0f, 18, 18, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(Minecraft.getInstance().font, title, 8, 6, 0x404040, false);
    }

    private float chrononRatio() {
        int capacity = menu.getChrononCapacity();
        return capacity <= 0 ? 0f : Math.clamp(menu.getChrononStored() / (float) capacity, 0f, 1f);
    }

    private List<Component> chrononTooltip() {
        return List.of(Component.literal("Chronon: " + menu.getChrononStored() + " / " + menu.getChrononCapacity()));
    }

    private List<Component> upgradeTooltip() {
        return List.of(Component.literal("Upgrade slot"), Component.literal("not implemented yet"));
    }

    private void stepMultiplier(int step) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return;
        mc.gameMode.handleInventoryButtonClick(menu.containerId,
                step > 0 ? MenuAccelerator.BUTTON_INCREASE : MenuAccelerator.BUTTON_DECREASE);
    }

    private void expandBlacklist() {
        // the reusable BlacklistWindow goes here
    }
}
