package org.moshang.tempusetchaos.client.gui.element;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.anim.Anims;
import org.moshang.tempusetchaos.client.gui.anim.Easing;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A control that steps through its options, drawing one option background at a time: left click steps forward, right
 * click back. A click only asks for the next option, it never becomes the displayed one by itself, so a value the
 * server owns can be read back through {@link #showing} and the sprite can never claim a mode the game is not in.
 */
@ParametersAreNonnullByDefault
public class InplaceToggleElement<T> extends UiElement {
    private final List<T> options;
    private final Consumer<T> onPick;
    private final List<ResourceLocation> optionBgs = new ArrayList<>();

    @Accessors(fluent = true, chain = true) @Setter
    private ResourceLocation baseBg = UiTextures.BUTTON;
    @Nullable
    private Supplier<T> showing;
    @Nullable
    private Function<T, Component> label;
    /** The option the player last asked for, shown while nothing reports a value back. */
    private int picked;

    public InplaceToggleElement(int x, int y, int width, int height, List<T> options, Consumer<T> onPick) {
        super(x, y, width, height);
        this.options = options;
        this.onPick = onPick;
        anims().add(Anims.hoverOverlay(0.25f, 100, Easing.LINEAR));
    }

    /** Where the value on show is read from: without it the control only knows what it last asked for itself. */
    public InplaceToggleElement<T> showing(Supplier<T> showing) {
        this.showing = showing;
        return this;
    }

    /** What the option on show is called, for the tooltip: a sprite alone cannot say which mode is active. */
    public InplaceToggleElement<T> labels(Function<T, Component> label) {
        this.label = label;
        return this;
    }

    public InplaceToggleElement<T> addOptionBg(ResourceLocation bg) {
        optionBgs.add(bg);
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiTextures.blitNineSliced(graphics, baseBg, 0, 0, width, height, false);
        int index = shownIndex();
        if (index < optionBgs.size()) graphics.blitSprite(optionBgs.get(index), 2, 2, width - 4, height - 4);
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        int step = button == 0 ? 1 : button == 1 ? -1 : 0;
        if (step == 0 || options.isEmpty()) return false;
        picked = Math.floorMod(shownIndex() + step, options.size());
        onPick.accept(options.get(picked));
        return true;
    }

    @Override
    public List<Component> getTooltip() {
        if (label == null || options.isEmpty()) return List.of();
        return List.of(label.apply(options.get(shownIndex())));
    }

    @Override
    public boolean takesFocus() {
        return false;
    }

    /** The option on show: what the source reports, else what the player last asked for. */
    private int shownIndex() {
        if (showing != null && !options.isEmpty()) {
            int index = options.indexOf(showing.get());
            if (index >= 0) return index;
        }
        return Math.clamp(options.size() - 1, 0, picked);
    }
}
