package org.moshang.tempusetchaos.client.gui;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.anim.AnimProps;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class WindowManager extends UiHost<IUiWindow> {
    @Getter
    private final Screen host;

    public WindowManager(Screen host) {
        this.host = host;
    }

    public void open(IUiWindow window) {
        add(window);
    }

    public boolean hasWindow() {
        return hasNodes();
    }

    public List<IUiWindow> getWindows() {
        return getNodes();
    }

    @Override
    public void bringToFront(IUiWindow window) {
        List<IUiWindow> group = withDescendants(window);
        nodes.removeAll(group);
        nodes.addAll(group);
    }

    @Override
    protected void onNodeAdded(IUiWindow window) {
        clampInto(window);
    }

    @Override
    protected void onHostResized() {
        for (IUiWindow window : nodes) {
            clampInto(window);
        }
    }

    @Override
    protected boolean applyClip(GuiGraphics graphics, IUiWindow window, @Nullable AnimProps props) {
        if (!window.isClipped()) return false;
        if (props == null) {
            graphics.enableScissor(window.getX(), window.getY(), window.getRight(), window.getBottom());
        } else {
            AnimProps.Rect rect = props.clip(window.getX(), window.getY(), window.getWidth(), window.getHeight());
            graphics.enableScissor(rect.left(), rect.top(), rect.right(), rect.bottom());
        }
        return true;
    }

    @Override
    protected boolean handlesOutsideClick(IUiWindow window) {
        return window.handlesOutsideClicks();
    }

    @Override
    protected boolean consumeKey(IUiWindow window, int keyCode, int scanCode, int modifiers) {
        if (!mc.options.keyInventory.matches(keyCode, scanCode)) return false;
        close(window);
        return true;
    }

    @Override
    @Nullable
    protected IUiWindow focusFallback(IUiWindow closing) {
        IUiWindow parent = closing.getParent();
        return parent != null && nodes.contains(parent) ? parent : getTop();
    }

    @Override
    protected List<IUiWindow> closeTargets(IUiWindow window) {
        return withDescendants(window);
    }

    private List<IUiWindow> withDescendants(IUiWindow root) {
        List<IUiWindow> result = new ArrayList<>();
        result.add(root);
        for (int i = 0; i < result.size(); i++) {
            IUiWindow parent = result.get(i);
            for (IUiWindow candidate : nodes) {
                if (candidate.getParent() == parent && !result.contains(candidate)) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    private void clampInto(IUiWindow window) {
        if (!sized()) return;
        int x = Math.clamp(window.getX(), 0, Math.max(0, hostWidth() - window.getWidth()));
        int y = Math.clamp(window.getY(), 0, Math.max(0, hostHeight() - window.getHeight()));
        if (x != window.getX() || y != window.getY()) window.setPosition(x, y);
    }
}
