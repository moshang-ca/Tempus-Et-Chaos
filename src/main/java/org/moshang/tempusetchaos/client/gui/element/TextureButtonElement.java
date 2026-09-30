package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.moshang.tempusetchaos.client.gui.anim.AnimValue;
import org.moshang.tempusetchaos.client.gui.anim.Easing;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

/** A textured button that lights up on hover. */
@ParametersAreNonnullByDefault
public class TextureButtonElement extends UiElement {
    private final Supplier<Component> label;
    private final Runnable onClick;
    private final AnimValue hover = new AnimValue(0f);

    public TextureButtonElement(int x, int y, Supplier<Component> label, Runnable onClick) {
        this.x = x;
        this.y = y;
        this.width = 60;
        this.height = 20;
        this.label = label;
        this.onClick = onClick;
    }

    @Override
    public void onHoverChanged(boolean hovered) {
        hover.to(hovered ? 1f : 0f, 120, Easing.EASE_OUT_QUAD);
    }

    @Override
    public void updateAnimation() {
        hover.update();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(UiTextures.BUTTON, 0, 0, getWidth(), getHeight(), 0f, 0f, getWidth(), getHeight(), getWidth(), getHeight());
        float highlight = hover.getValue();
        if (highlight > 0.01f) graphics.fill(0, 0, getWidth(), getHeight(), (int) (highlight * 0x40) << 24 | 0xFFFFFF);
        graphics.drawCenteredString(Minecraft.getInstance().font, label.get(), getWidth() / 2, (getHeight() - 8) / 2, 0xFFFFFF);
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        onClick.run();
        return true;
    }
}
