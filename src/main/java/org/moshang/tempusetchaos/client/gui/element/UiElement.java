package org.moshang.tempusetchaos.client.gui.element;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import org.moshang.tempusetchaos.client.gui.IUiNode;
import org.moshang.tempusetchaos.client.gui.anim.AnimProps;

import javax.annotation.ParametersAreNonnullByDefault;

/** A node hosted by a window, positioned in the window's own space, its origin being its top left. */
@Getter
@ParametersAreNonnullByDefault
public abstract class UiElement implements IUiNode {
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    @Setter
    protected boolean visible = true;
    protected final AnimProps renderProps = new AnimProps();

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
}
