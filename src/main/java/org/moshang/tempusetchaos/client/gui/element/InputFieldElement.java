package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.anim.Anims;
import org.moshang.tempusetchaos.client.gui.anim.Easing;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A text field bound to a value: it follows the supplier while it is not being edited, and hands the whole edit over
 * on enter, on losing focus or on close rather than on every keystroke. Editing, selection, word deletes and the
 * clipboard come from vanilla's {@link EditBox}.
 */
@ParametersAreNonnullByDefault
public class InputFieldElement extends UiElement {
    private static final int PADDING = 4;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int CURSOR_COLOR = 0xFFD0D0D0;
    private static final int HINT_COLOR = 0xFF808080;
    private static final int SELECTION_COLOR = 0x80303080;

    /** The value this field follows, null when it owns its own text. */
    @Nullable
    private final Supplier<String> value;
    @Nullable
    private final Consumer<String> onCommit;
    private final EditBox editor;
    private @Nullable String hint;
    private boolean focused;
    private boolean handedOff;
    private int tickCount;
    /** Where the cursor was before the selection started: the editor only hands out the selected text. */
    private int selectionAnchor = -1;

    public InputFieldElement(int x, int y, int width, String text) {
        this(x, y, width, 16, text);
    }

    public InputFieldElement(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        this.value = null;
        this.onCommit = null;
        this.editor = newEditor(width, height, text);
        anims().add(Anims.hoverOverlay(0.25f, 120, Easing.EASE_OUT_QUAD));
    }

    public InputFieldElement(int x, int y, int width, Supplier<String> value, Consumer<String> onCommit) {
        this(x, y, width, 16, value, onCommit);
    }

    public InputFieldElement(int x, int y, int width, int height, Supplier<String> value, Consumer<String> onCommit) {
        super(x, y, width, height);
        this.value = value;
        this.onCommit = onCommit;
        this.editor = newEditor(width, height, value.get());
        anims().add(Anims.hoverOverlay(0.25f, 120, Easing.EASE_OUT_QUAD));
    }

    private static EditBox newEditor(int width, int height, String text) {
        EditBox editor = new EditBox(Minecraft.getInstance().font, 0, 0, width, height, Component.empty());
        editor.setBordered(false);
        editor.setValue(text);
        return editor;
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

    /** Replaces the text the field is showing. */
    public void setValue(String text) {
        editor.setValue(text);
    }

    @Override
    public void tick() {
        tickCount++;
        if (value != null && !focused && !editor.getValue().equals(value.get())) editor.setValue(value.get());
        if (editor.getHighlighted().isEmpty()) selectionAnchor = editor.getCursorPosition();
    }

    @Override
    public void onFocusChanged(boolean focused) {
        super.onFocusChanged(focused);
        boolean editing = this.focused;
        this.focused = focused;
        this.tickCount = 0;
        this.handedOff = false;
        editor.setFocused(focused);
        if (editing && !focused) commit();
    }

    /** A host that goes away takes the field with it, and the edit it was holding is still the player's last word. */
    @Override
    public void onClose() {
        if (focused) commit();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editor.canConsumeInput()) return false;
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            handedOff = true;
            return true;
        }
        handedOff = false;
        return editor.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean releasesFocus() {
        return handedOff;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return editor.canConsumeInput() && editor.charTyped(codePoint, modifiers);
    }

    /** Empty text withdraws the edit of a bound field; a field that owns its text keeps the empty one. */
    private void commit() {
        String text = editor.getValue();
        if (text.isEmpty() && value != null) {
            editor.setValue(value.get());
            return;
        }
        if (onCommit != null) onCommit.accept(text);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        graphics.blitSprite(UiTextures.INPUT_FIELD, 0, 0, getWidth(), getHeight());

        String text = editor.getValue();
        int inner = getWidth() - PADDING * 2;
        int textY = (getHeight() - 8) / 2;

        if (text.isEmpty() && hint != null && !focused) {
            graphics.drawCenteredString(font, hint, getWidth() / 2, textY, HINT_COLOR);
            return;
        }

        int cursor = editor.getCursorPosition();
        int scroll = scrollOf(font, text, cursor, inner);
        String visible = font.plainSubstrByWidth(text.substring(scroll), inner);
        int left = PADDING + (inner - font.width(visible)) / 2;

        renderSelection(graphics, font, text, scroll, inner, textY, left);
        graphics.drawString(font, visible, left, textY, TEXT_COLOR, false);

        if (focused && (tickCount / 6) % 2 == 0) {
            int cursorX = left + font.width(text.substring(scroll, Mth.clamp(cursor, scroll, text.length())));
            graphics.fill(cursorX, textY - 1, cursorX + 1, textY + 9, CURSOR_COLOR);
        }
    }

    /** How many leading characters are scrolled out of view, so the cursor stays inside the field. */
    private static int scrollOf(Font font, String value, int cursor, int inner) {
        int scroll = 0;
        while (scroll < cursor && font.width(value.substring(scroll, cursor)) > inner) scroll++;
        return scroll;
    }

    /** Draws the highlight over the selected characters, cut to the part of them the field is showing. */
    private void renderSelection(GuiGraphics graphics, Font font, String value, int scroll, int inner, int textY, int left) {
        String highlighted = editor.getHighlighted();
        if (highlighted.isEmpty()) return;

        // The cursor sits at one end of the selection and the selected text alone does not say which end, so the anchor
        // kept in tick decides between the two readings; both are only taken when the text actually matches there.
        int length = highlighted.length();
        int cursor = editor.getCursorPosition();
        int start = selectionAnchor >= 0 && selectionAnchor + length == cursor
                && value.startsWith(highlighted, selectionAnchor) ? selectionAnchor : cursor;
        if (start < 0 || start + length > value.length() || !value.startsWith(highlighted, start)) return;

        int visibleStart = Mth.clamp(start, scroll, value.length());
        int visibleEnd = Mth.clamp(start + length, scroll, value.length());
        int from = font.width(value.substring(scroll, visibleStart));
        int to = from + font.width(value.substring(visibleStart, visibleEnd));
        from = Mth.clamp(from, 0, inner);
        to = Mth.clamp(to, 0, inner);
        if (to > from) graphics.fill(RenderType.guiTextHighlight(), left + from, textY - 1, left + to, textY + 9, SELECTION_COLOR);
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        return button == 0;
    }
}
