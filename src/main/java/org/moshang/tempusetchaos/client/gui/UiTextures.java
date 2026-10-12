package org.moshang.tempusetchaos.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.GlStateBackup;
import org.moshang.tempusetchaos.TempusEtChaos;

@SuppressWarnings("unused")
public final class UiTextures {
    public static final ResourceLocation BASE_INVENTORY = ui("base_inventory.png");
    public static final ResourceLocation UPGRADE_SLOT = uiSprite("upgrade_slot");
    public static final ResourceLocation FRONT_ICON = ui("front_icon.png");
    public static final ResourceLocation PANEL = uiSprite("panel");
    public static final ResourceLocation SLOT = uiSprite("slot");
    public static final ResourceLocation CHANNEL = uiSprite("chronon_channel");
    public static final ResourceLocation BUTTON = uiSprite("button");
    public static final ResourceLocation INPUT_FIELD = uiSprite("input_field");

    private UiTextures() {}

    public static ResourceLocation ui(String name) {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "textures/gui/ui/" + name);
    }

    public static ResourceLocation uiSprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "ui/"+ name);
    }

    public static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, name);
    }

    /**
     * Draws a nine-slice sprite with its centre stretched, where vanilla's {@code blitSprite} tiles it instead.
     */
    // TODO: Remove this on 1.21.4+
    public static void blitNineSliced(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height, boolean tile) {
        if (tile) {
            graphics.blitSprite(sprite, x, y, width, height);
            return;
        }

        GuiSpriteManager sprites = Minecraft.getInstance().getGuiSprites();
        if (!(sprites.getSpriteScaling(sprites.getSprite(sprite)) instanceof
                GuiSpriteScaling.NineSlice(int texWidth, int texHeight, GuiSpriteScaling.NineSlice.Border border))) return;

        // a target smaller than the borders keeps the corners and drops what will not fit
        int sliceLeft = Math.min(border.left(), width / 2);
        int sliceRight = Math.min(border.right(), width - sliceLeft);
        int sliceTop = Math.min(border.top(), height / 2);
        int sliceBottom = Math.min(border.bottom(), height - sliceTop);
        ResourceLocation texture = spriteFile(sprite);

        // lt - rt - lb - rb
        graphics.blit(texture, x, y, sliceLeft, sliceTop, 0, 0, sliceLeft, sliceTop, texWidth, texHeight);
        graphics.blit(texture, x + width - sliceRight, y, sliceRight, sliceTop,
                texWidth - sliceRight, 0, sliceRight, sliceTop, texWidth, texHeight);
        graphics.blit(texture, x, y + height - sliceBottom, sliceLeft, sliceBottom,
                0, texHeight - sliceBottom, sliceLeft, sliceBottom, texWidth, texHeight);
        graphics.blit(texture, x + width - sliceRight, y + height - sliceBottom, sliceRight, sliceBottom,
                texWidth - sliceRight, texHeight - sliceBottom, sliceRight, sliceBottom, texWidth, texHeight);

        // t - b - l - r
        graphics.blit(texture, x + sliceLeft, y,
                width - sliceLeft - sliceRight, sliceTop,
                sliceLeft, 0,
                texWidth - sliceLeft - sliceRight, sliceTop,
                texWidth, texHeight);
        graphics.blit(texture, x + sliceLeft, y + height - sliceBottom,
                width - sliceLeft - sliceRight, sliceBottom,
                sliceLeft, texHeight - sliceBottom,
                texWidth - sliceLeft - sliceRight, sliceBottom,
                texWidth, texHeight);
        graphics.blit(texture, x, y + sliceTop,
                sliceLeft, height - sliceTop - sliceBottom,
                0, sliceTop,
                sliceLeft, texHeight - sliceTop - sliceBottom,
                texWidth, texHeight);
        graphics.blit(texture, x + width - sliceRight, y + sliceTop,
                sliceRight, height - sliceTop - sliceBottom,
                texWidth - sliceRight, sliceTop,
                sliceRight, texHeight - sliceTop - sliceBottom,
                texWidth, texHeight);

        // center
        graphics.blit(texture, x + sliceLeft, y + sliceTop,
                width - sliceLeft - sliceRight, height - sliceTop - sliceBottom,
                sliceLeft, sliceTop,
                texWidth - sliceLeft - sliceRight, texHeight - sliceTop - sliceBottom,
                texWidth, texHeight);
    }

    /** The file a gui sprite is stitched from. */
    public static ResourceLocation spriteFile(ResourceLocation sprite) {
        return ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(),
                "textures/gui/sprites/" + sprite.getPath() + ".png");
    }

    public static void blitTranslucentSprite(GuiGraphics graphics, ResourceLocation texture,
                                             int textureWidth, int textureHeight, int uPosition, int vPosition,
                                             int x, int y, int uWidth, int vHeight) {
        GlStateBackup backup = new GlStateBackup();
        RenderSystem.backupGlState(backup);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blitSprite(texture, textureWidth, textureHeight, uPosition, vPosition, x, y, uWidth, vHeight);

        RenderSystem.restoreGlState(backup);
    }
}
