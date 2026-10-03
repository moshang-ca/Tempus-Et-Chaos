package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.anim.Anims;
import org.moshang.tempusetchaos.client.gui.anim.Easing;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

/** A textured button that lights up on hover. The hover wash is an attached animation, not baked in. */
@ParametersAreNonnullByDefault
public class TextureButtonElement extends TextureElement {
    private final Supplier<Component> label;
    private final Runnable onClick;

    public TextureButtonElement(int x, int y, int width, int height, ResourceLocation texture, Supplier<Component> label, Runnable onClick) {
        super(x, y, width, height, texture);
        this.label = label;
        this.onClick = onClick;
        anims().add(Anims.hoverOverlay(0.25f, 120, Easing.EASE_OUT_QUAD));
    }

    public TextureButtonElement(int x, int y, Supplier<Component> label, Runnable onClick) {
        this(x, y, 60, 20, UiTextures.BUTTON, label, onClick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(texture, 0, 0, getWidth(), getHeight());
        graphics.drawCenteredString(Minecraft.getInstance().font, label.get(), getWidth() / 2, (getHeight() - 8) / 2, 0xFFFFFF);
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        onClick.run();
        return true;
    }
}
