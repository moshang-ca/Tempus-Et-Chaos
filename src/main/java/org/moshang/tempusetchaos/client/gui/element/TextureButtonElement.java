package org.moshang.tempusetchaos.client.gui.element;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.moshang.tempusetchaos.client.gui.UiHost;
import org.moshang.tempusetchaos.client.gui.anim.Anims;
import org.moshang.tempusetchaos.client.gui.anim.Easing;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

/** A textured button that lights up on hover. */
@ParametersAreNonnullByDefault
public class TextureButtonElement extends TextureElement {
    @Nullable
    private final Supplier<Component> label;
    private final Runnable onClick;
    private final boolean needScratch;

    /** Whether the button takes clicks right now, for a control that only makes sense with a selection, say. */
    @Accessors(fluent = true, chain = true) @Setter
    private Supplier<Boolean> enabled = () -> true;

    public TextureButtonElement(int x, int y, int width, int height, ResourceLocation texture,
                                boolean needScratch, @Nullable Supplier<Component> label, Runnable onClick) {
        super(x, y, width, height, texture);
        this.label = label;
        this.onClick = onClick;
        this.needScratch = needScratch;
        anims().add(Anims.hoverOverlay(0.25f, 120, Easing.EASE_OUT_QUAD));
    }

    public TextureButtonElement(int x, int y, int width, int height, boolean needScratch, ResourceLocation texture, Runnable onClick) {
        this(x, y, width, height, texture, needScratch, null, onClick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (needScratch)
            graphics.blitSprite(texture, 0, 0, getWidth(), getHeight());
        else
            graphics.blit(texture, 0, 0, getWidth(), getHeight(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
        if (label != null)
            graphics.drawCenteredString(Minecraft.getInstance().font, label.get(), getWidth() / 2, (getHeight() - 8) / 2,
                    enabled.get() ? 0xFFFFFF : 0xFF808080);
        if (!enabled.get()) {
            // the dim has to go over the texture and the label both, and items sit above the plain gui layer
            UiHost.clearContentDepth(graphics);
            graphics.fill(0, 0, getWidth(), getHeight(), 0xA0000000);
        }
    }

//    @Override
//    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
//        if (button != 0 || !enabled.get()) return false;
//        onClick.run();
//        return true;
//    }

    @Override
    public boolean isMouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled.get()) return false;
        onClick.run();
        return true;
    }
}
