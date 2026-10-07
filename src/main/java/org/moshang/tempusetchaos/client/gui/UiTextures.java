package org.moshang.tempusetchaos.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.GlStateBackup;
import org.moshang.tempusetchaos.TempusEtChaos;

@SuppressWarnings("unused")
public final class UiTextures {
    public static final ResourceLocation BASE_INVENTORY = ui("base_inventory.png");
    public static final ResourceLocation PANEL = ui("panel.png");
    public static final ResourceLocation SLOT = ui("slot.png");
    public static final ResourceLocation BUTTON = uiSprite("button");
    public static final ResourceLocation INPUT_FIELD = uiSprite("input_field");
    public static final ResourceLocation CHANNEL = ui("chronon_channel.png");
    public static final ResourceLocation FRONT_ICON = ui("front_icon.png");

    public static final ResourceLocation CHRONON = uiSprite("chronon");

    private UiTextures() {}

    private static ResourceLocation ui(String name) {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "textures/gui/ui/" + name);
    }

    private static ResourceLocation uiSprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "ui/"+ name);
    }

//    public static void blitNineSliced(GuiGraphics graphics, ResourceLocation texture,
//                                      int x, int y, int width, int height,
//                                      int u, int v, int texWidth, int texHeight,
//                                      int sliceLeft, int sliceRight, int sliceTop, int sliceBottom) {
//        // lt - rt - lb - rb
//        graphics.blit(texture, x, y, sliceLeft, sliceTop, u, v, sliceLeft, sliceTop, texWidth, texHeight);
//        graphics.blit(texture, x + width - sliceRight, y, sliceRight, sliceTop,
//                u + texWidth - sliceRight, v, sliceRight, sliceTop, texWidth, texHeight);
//        graphics.blit(texture, x, y + height - sliceBottom, sliceLeft, sliceBottom,
//                u, v + texHeight - sliceBottom, sliceLeft, sliceBottom, texWidth, texHeight);
//        graphics.blit(texture, x + width - sliceRight, y + height - sliceBottom, sliceRight, sliceBottom,
//                u + texWidth - sliceRight, v + texHeight - sliceBottom, sliceRight, sliceBottom, texWidth, texHeight);
//
//        // t - b - l - r
//        graphics.blit(texture, x + sliceLeft, y,
//                width - sliceLeft - sliceRight, sliceTop,
//                u + sliceLeft, v,
//                texWidth - sliceLeft - sliceRight, sliceTop,
//                texWidth, texHeight);
//        graphics.blit(texture, x + sliceLeft, y + height - sliceBottom,
//                width - sliceLeft - sliceRight, sliceBottom,
//                u + sliceLeft, v + texHeight - sliceBottom,
//                texWidth - sliceLeft - sliceRight, sliceBottom,
//                texWidth, texHeight);
//        graphics.blit(texture, x, y + sliceTop,
//                sliceLeft, height - sliceTop - sliceBottom,
//                u, v + sliceTop,
//                sliceLeft, texHeight - sliceTop - sliceBottom,
//                texWidth, texHeight);
//        graphics.blit(texture, x + width - sliceRight, y + sliceTop,
//                sliceRight, height - sliceTop - sliceBottom,
//                u + texWidth - sliceRight, v + sliceTop,
//                sliceRight, texHeight - sliceTop - sliceBottom,
//                texWidth, texHeight);
//
//        // center
//        graphics.blit(texture, x + sliceLeft, y + sliceTop,
//                width - sliceLeft - sliceRight, height - sliceTop - sliceBottom,
//                u + sliceLeft, v + sliceTop,
//                texWidth - sliceLeft - sliceRight, texHeight - sliceTop - sliceBottom,
//                texWidth, texHeight);
//    }
//
//    public static void button(GuiGraphics graphics, int x, int y, int width, int height) {
//        blitNineSliced(graphics, BUTTON, x, y, width, height, 0, 0, 32, 32, 4, 4, 4, 4);
//    }

    public static void blitTranslucentSprite(GuiGraphics graphics, ResourceLocation texture,
                                             int textureWidth, int textureHeight, int uPosition, int vPosition,
                                             int x, int y, int uWidth, int vHeight) {
        GlStateBackup backup = new GlStateBackup();
        RenderSystem.backupGlState(backup);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blitSprite(texture, textureWidth, textureHeight, uPosition, vPosition, 1, y, uWidth, vHeight);

        RenderSystem.restoreGlState(backup);
    }
}
