package org.moshang.tempusetchaos.client.gui.element;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.moshang.tempusetchaos.client.gui.UiHost;
import org.moshang.tempusetchaos.client.gui.UiTextures;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A strip of mutually exclusive options: exactly one is selected, a click on another picks it and the picked one is
 * reported. It divides its own box, so the whole strip is one hit test.
 */
@ParametersAreNonnullByDefault
public class ToggleElement<T> extends UiElement {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SELECTED_WASH = 0x60FFFFFF;
    private static final int HOVER_WASH = 0x30FFFFFF;

    private final List<T> options;
    private final Function<T, Component> label;
    private final Consumer<T> onPick;

    @Setter
    private ResourceLocation optionBg = UiTextures.INPUT_FIELD;
    @Setter @Getter
    private T selected;
    private int hovered = -1;

    public ToggleElement(int x, int y, int width, int height, List<T> options,
                         Function<T, Component> label, Consumer<T> onPick) {
        super(x, y, width, height);
        this.options = options;
        this.label = label;
        this.onPick = onPick;
        this.selected = options.isEmpty() ? null : options.getFirst();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (options.isEmpty()) return;
        hovered = indexAt(mouseX, mouseY);

        for (int i = 0; i < options.size(); i++) {
            UiTextures.blitNineSliced(graphics, optionBg, segmentX(i), 0, segmentWidth(i), getHeight(), false);
        }

        // the washes go over a texture, and items sit above the plain gui layer, so the depth has to go first
        UiHost.clearContentDepth(graphics);
        if (hovered >= 0) {
            graphics.fill(segmentX(hovered), 0, segmentX(hovered) + segmentWidth(hovered), getHeight(), HOVER_WASH);
        }
        int chosen = options.indexOf(selected);
        if (chosen >= 0) {
            UiTextures.blitNineSliced(graphics, UiTextures.BUTTON, segmentX(chosen), 0, segmentWidth(0), getHeight(), false);
        }
        for (int i = 0; i < options.size(); i++) {
            graphics.drawCenteredString(Minecraft.getInstance().font, label.apply(options.get(i)),
                    segmentX(i) + segmentWidth(i) / 2, (getHeight() - 8) / 2, TEXT_COLOR);
        }
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int index = indexAt(mouseX, mouseY);
        if (index < 0) return false;
        T picked = options.get(index);
        if (picked.equals(selected)) return true;
        selected = picked;
        onPick.accept(picked);
        return true;
    }

    @Override
    public boolean takesFocus() {
        return false;
    }

    private int segmentX(int index) {
        return index * (getWidth() / options.size());
    }

    /** The last segment takes what the integer division left over, so the strip always fills its box. */
    private int segmentWidth(int index) {
        int x = segmentX(index);
        return index == options.size() - 1 ? getWidth() - x : getWidth() / options.size();
    }

    private int indexAt(double mouseX, double mouseY) {
        if (options.isEmpty() || mouseX < 0 || mouseY < 0 || mouseX >= getWidth() || mouseY >= getHeight()) return -1;
        int segment = getWidth() / options.size();
        if (segment <= 0) return -1;
        return Math.min(options.size() - 1, (int) (mouseX / segment));
    }
}
