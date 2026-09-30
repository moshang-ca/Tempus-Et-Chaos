package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.moshang.tempusetchaos.client.gui.anim.AnimValue;
import org.moshang.tempusetchaos.client.gui.anim.Easing;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** A value field: left click steps up, right click steps down. */
@ParametersAreNonnullByDefault
public class InputFieldElement extends UiElement {
    private final Supplier<String> text;
    private final IntConsumer onStep;
    private final AnimValue hover = new AnimValue(0f);

    public InputFieldElement(int x, int y, Supplier<String> text, IntConsumer onStep) {
        this.x = x;
        this.y = y;
        this.width = 100;
        this.height = 16;
        this.text = text;
        this.onStep = onStep;
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
        graphics.blit(UiTextures.INPUT_FIELD, 0, 0, getWidth(), getHeight(), 0f, 0f, getWidth(), getHeight(), getWidth(), getHeight());
        float highlight = hover.getValue();
        if (highlight > 0.01f) graphics.fill(0, 0, getWidth(), getHeight(), (int) (highlight * 0x40) << 24 | 0xFFFFFF);
        graphics.drawCenteredString(Minecraft.getInstance().font, text.get(), getWidth() / 2, (getHeight() - 8) / 2, 0xFFFFFF);
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            onStep.accept(1);
            return true;
        }
        if (button == 1) {
            onStep.accept(-1);
            return true;
        }
        return false;
    }
}
