package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.anim.Anims;
import org.moshang.tempusetchaos.client.gui.anim.Easing;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A text field. Editing, selection, word deletes and the clipboard come from vanilla's {@link EditBox}.
 */
@ParametersAreNonnullByDefault
public class InputFieldElement extends UiElement {
    private static final int PADDING = 4;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int CURSOR_COLOR = 0xFFD0D0D0;
    private static final int HINT_COLOR = 0xFF808080;
    private static final int SELECTION_COLOR = 0x80303080;

    private final EditBox editor;
    private @Nullable String hint;
    private boolean focused;
    private int tickCount;

    public InputFieldElement(int x, int y, int width, String initial, Consumer<String> onChanged) {
        this(x, y, width, 16, initial, onChanged);
    }

    public InputFieldElement(int x, int y, int width, int height, String initial, Consumer<String> onChanged) {
        super(x, y, width, height);
        this.editor = new EditBox(Minecraft.getInstance().font, 0, 0, width, height, Component.empty());
        this.editor.setBordered(false);
        this.editor.setValue(initial);
        this.editor.setResponder(onChanged);
        anims().add(Anims.hoverOverlay(0.25f, 120, Easing.EASE_OUT_QUAD));
    }

    public InputFieldElement maxLength(int maxLength) {
        editor.setMaxLength(maxLength);
        return this;
    }

    public InputFieldElement filter(Predicate<String> filter) {
        editor.setFilter(filter);
        return this;
    }

    public InputFieldElement hint(String hint) {
        this.hint = hint;
        return this;
    }

    public String getValue() {
        return editor.getValue();
    }

    public void setValue(String value) {
        editor.setValue(value);
    }

    @Override
    public void tick() {
        tickCount++;
    }

    @Override
    public void onFocusChanged(boolean focused) {
        super.onFocusChanged(focused);
        this.focused = focused;
        this.tickCount = 0;
        editor.setFocused(focused);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return editor.canConsumeInput() && editor.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return editor.canConsumeInput() && editor.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        graphics.blitSprite(UiTextures.INPUT_FIELD, 0, 0, getWidth(), getHeight());

        String value = editor.getValue();
        int inner = getWidth() - PADDING * 2;
        int textY = (getHeight() - 8) / 2;

        if (value.isEmpty() && hint != null && !focused) {
            graphics.drawCenteredString(font, hint, getWidth() / 2, textY, HINT_COLOR);
            return;
        }

        // EditBox keeps its scroll offset private, so it is derived from where the cursor sits.
        int cursor = editor.getCursorPosition();
        int scroll = scrollOf(font, value, cursor, inner);
        String visible = font.plainSubstrByWidth(value.substring(scroll), inner);
        int left = PADDING + (inner - font.width(visible)) / 2;

        renderSelection(graphics, font, value, scroll, inner, textY, left);
        graphics.drawString(font, visible, left, textY, TEXT_COLOR, false);

        if (focused && (tickCount / 6) % 2 == 0) {
            int cursorX = left + font.width(value.substring(scroll, Mth.clamp(cursor, scroll, value.length())));
            graphics.fill(cursorX, textY - 1, cursorX + 1, textY + 9, CURSOR_COLOR);
            System.out.println(scroll);
        }
    }

    /** How many leading characters are scrolled out of view, so the cursor stays inside the field. */
    private static int scrollOf(Font font, String value, int cursor, int inner) {
        int scroll = 0;
        while (scroll < cursor && font.width(value.substring(scroll, cursor)) > inner) scroll++;
        return scroll;
    }

    private void renderSelection(GuiGraphics graphics, Font font, String value, int scroll, int inner, int textY, int left) {
        String highlighted = editor.getHighlighted();
        int start = highlighted.isEmpty() ? -1 : value.indexOf(highlighted);
        if (start < 0) return;

        int from = font.width(value.substring(scroll, Mth.clamp(start, scroll, value.length())));
        int to = from + font.width(highlighted.substring(Math.max(0, scroll - start)));
        from = Mth.clamp(from, 0, inner);
        to = Mth.clamp(to, 0, inner);
        if (to > from) graphics.fill(RenderType.guiTextHighlight(), left + from, textY - 1, left + to, textY + 9, SELECTION_COLOR);
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        return button == 0;
    }
}
