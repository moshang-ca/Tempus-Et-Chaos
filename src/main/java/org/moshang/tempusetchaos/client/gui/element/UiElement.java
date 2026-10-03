package org.moshang.tempusetchaos.client.gui.element;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.moshang.tempusetchaos.client.gui.anim.AnimSet;
import org.moshang.tempusetchaos.client.gui.anim.Animated;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/** A node hosted by a window, positioned in the window's own space, its origin being its top left. */
@Getter
@ParametersAreNonnullByDefault
public abstract class UiElement implements Animated {
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    @Setter
    protected boolean visible = true;
    private boolean hovered;
    @Getter(AccessLevel.NONE)
    private final AnimSet anims = new AnimSet();

    public UiElement(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public AnimSet anims() {
        return anims;
    }

    @Override
    public void onHoverChanged(boolean hovered) {
        this.hovered = hovered;
        Animated.super.onHoverChanged(hovered);
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Drawn by the hosting window after every element, so nothing covers it. */
    public List<Component> getTooltip() {
        return List.of();
    }

    @Override
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
}
