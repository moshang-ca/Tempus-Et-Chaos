package org.moshang.tempusetchaos.client.gui.window;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.client.gui.ContainerWindow;

public class SimpleContainerWindow<T extends AbstractContainerMenu> extends ContainerWindow<T> {
    private final ResourceLocation bgTexture;
    private final int texWidth;
    private final int texHeight;

    public SimpleContainerWindow(T menu, int width, int height, ResourceLocation bgTexture) {
        this(menu, width, height, bgTexture, width, height);
    }

    /**
     * @param texWidth The total width of texture, default to {@code width}.
     * @param texHeight The total height of texture, default to {@code height}.
     * */
    public SimpleContainerWindow(T menu, int width, int height, ResourceLocation bgTexture, int texWidth, int texHeight) {
        super(menu, width, height);
        this.bgTexture = bgTexture;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(bgTexture, 0, 0, getWidth(), getHeight(), 0f, 0f, getWidth(), getHeight(), texWidth, texHeight);
    }
}
