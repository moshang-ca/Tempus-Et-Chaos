package org.moshang.tempusetchaos.client.gui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.moshang.tempusetchaos.client.gui.anim.AnimSet;
import org.moshang.tempusetchaos.client.gui.anim.Animated;
import org.moshang.tempusetchaos.client.gui.element.UiElement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * A window that hosts elements. Elements live in the window's own space, the host translates the pose for them.
 */
@Accessors(fluent = true, chain = true)
@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public abstract class AbstractUiWindow implements IUiWindow, Animated {
    /** The window's own element space, so whoever holds the window can map an element's box out to the screen. */
    @Getter
    protected final UiHost<UiElement> elements = new UiHost<>();
    @Getter
    private final AnimSet anims = new AnimSet();

    @Accessors(fluent = false) @Getter
    protected IUiWindow parent;
    @Setter @Getter
    protected boolean canDrag = false;

    @Override
    public void init(Minecraft mc, int screenWidth, int screenHeight) {
        elements.init(getWidth(), getHeight());
    }

    @Override
    public void onResize(int screenWidth, int screenHeight) {
        elements.onResize(getWidth(), getHeight());
    }

    @Override
    public void tick() {
        elements.tick();
    }

    /** Both halves matter: the window's own animations and every element's. */
    @Override
    public void updateAnimation() {
        anims.tick();
        elements.updateAnimations();
    }

    @Override
    public void onClose() {
        elements.disposeAll();
    }

    @Override
    public void onRemoved() {
        elements.disposeAll();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderContent(graphics, mouseX, mouseY, partialTick);
        elements.render(graphics, mouseX, mouseY, partialTick);
    }

    /** The host calls this outside the window's clip, so the tooltip can reach past the window bounds. */
    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTooltips(graphics, mouseX, mouseY);
    }

    /** Element tooltips are drawn after every element, otherwise later elements would paint over them. */
    protected void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        UiElement hovered = elements.getHovered();
        if (hovered == null) return;
        List<Component> tooltip = hovered.getTooltip();
        if (tooltip.isEmpty()) return;
        List<FormattedCharSequence> lines = tooltip.stream().map(Component::getVisualOrderText).toList();
        graphics.renderTooltip(Minecraft.getInstance().font, lines, mouseX, mouseY);
    }

    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        elements.mouseMoved(mouseX, mouseY);
        onMouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        return elements.isMouseClicked(mouseX, mouseY, button) || onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseReleased(double mouseX, double mouseY, int button) {
        return elements.isMouseReleased(mouseX, mouseY, button) || onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return elements.isMouseDragged(mouseX, mouseY, button, dragX, dragY)
                || onMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return elements.isMouseScrolled(mouseX, mouseY, scrollX, scrollY)
                || onMouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return elements.keyPressed(keyCode, scanCode, modifiers) || onKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return elements.keyReleased(keyCode, scanCode, modifiers) || onKeyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return elements.charTyped(codePoint, modifiers) || onCharTyped(codePoint, modifiers);
    }

    protected void onMouseMoved(double mouseX, double mouseY) {}

    protected boolean onMouseClicked(double mouseX, double mouseY, int button) { return false; }

    protected boolean onMouseReleased(double mouseX, double mouseY, int button) { return false; }

    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { return false; }

    protected boolean onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { return false; }

    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    protected boolean onKeyReleased(int keyCode, int scanCode, int modifiers) { return false; }

    protected boolean onCharTyped(char codePoint, int modifiers) { return false; }
}
