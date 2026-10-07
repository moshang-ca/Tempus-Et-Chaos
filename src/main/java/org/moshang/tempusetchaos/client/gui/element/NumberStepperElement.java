package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.anim.Anims;
import org.moshang.tempusetchaos.client.gui.anim.Easing;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** A value field: left click steps up, right click steps down. The hover wash is an attached animation. */
@ParametersAreNonnullByDefault
public class NumberStepperElement extends UiElement {
    private final Supplier<String> text;
    private final IntConsumer onStep;

    public NumberStepperElement(int x, int y, Supplier<String> text, IntConsumer onStep) {
        this(x, y, 100, 16, text, onStep);
    }

    public NumberStepperElement(int x, int y, int width, int height, Supplier<String> text, IntConsumer onStep) {
        super(x, y, width, height);
        this.text = text;
        this.onStep = onStep;
        anims().add(Anims.hoverOverlay(0.25f, 120, Easing.EASE_OUT_QUAD));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(UiTextures.INPUT_FIELD, 0, 0, getWidth(), getHeight());
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
