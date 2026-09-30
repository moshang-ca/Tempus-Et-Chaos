package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.resources.ResourceLocation;
import org.moshang.tempusetchaos.TempusEtChaos;

/** The hand drawn 1:1 textures under textures/gui/ui, each blitted with its own size. */
@SuppressWarnings("unused")
public final class UiTextures {
    public static final ResourceLocation BASE_INVENTORY = ui("base_inventory.png");
    public static final ResourceLocation PANEL = ui("panel.png");
    public static final ResourceLocation SLOT = ui("slot.png");
    public static final ResourceLocation BUTTON = ui("button.png");
    public static final ResourceLocation INPUT_FIELD = ui("input_field.png");
    public static final ResourceLocation CHRONON_CHANNEL = ui("chronon_channel.png");

    private UiTextures() {}

    private static ResourceLocation ui(String name) {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "textures/gui/ui/" + name);
    }
}
