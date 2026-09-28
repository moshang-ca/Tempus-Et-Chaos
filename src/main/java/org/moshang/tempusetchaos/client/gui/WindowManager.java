package org.moshang.tempusetchaos.client.gui;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
public class WindowManager {
    private final Minecraft mc;
    @Getter
    private final Screen host;
    private final List<IUIWindow> windows = new ArrayList<>();
    private final List<IUIWindow> pendingAdd = new ArrayList<>();
    private final List<IUIWindow> pendingRemove = new ArrayList<>();
    private final List<IUIWindow> needsInit = new ArrayList<>();

    private int screenWidth;
    private int screenHeight;
    private boolean sized;
    private int dispatchDepth;

    @Nullable
    private IUIWindow dragging;

    public WindowManager(Screen host) {
        this.mc = Minecraft.getInstance();
        this.host = host;
    }

    public void init(int screenWidth, int screenHeight) {
        updateSize(screenWidth, screenHeight);
        flushNeedsInit();
    }

    public void onResize(int screenWidth, int screenHeight) {
        updateSize(screenWidth, screenHeight);
        flushNeedsInit();
        for (IUIWindow window : windows) {
            window.onResize(screenWidth, screenHeight);
        }
        for (IUIWindow window : windows) {
            clampInto(window);
        }
    }

    public void tick() {
        enter();
        try {
            for (IUIWindow window : windows) {
                if (window.isVisible()) window.tick();
            }
        } finally {
            exit();
        }
    }

    public void open(IUIWindow window) {
        if (dispatchDepth > 0) {
            pendingAdd.add(window);
            return;
        }
        doOpen(window);
    }

    public void close(IUIWindow window) {
        if (dispatchDepth > 0) {
            pendingRemove.add(window);
            return;
        }
        doClose(window);
    }

    public void closeTop() {
        IUIWindow top = getTop();
        if (top != null) close(top);
    }

    public void closeAll() {
        List<IUIWindow> doomed = detachAll();
        for (int i = doomed.size() - 1; i >= 0; i--) {
            doomed.get(i).onClose();
        }
    }

    public void disposeAll() {
        List<IUIWindow> doomed = detachAll();
        for (int i = doomed.size() - 1; i >= 0; i--) {
            doomed.get(i).onRemoved();
        }
    }

    public boolean hasWindow() {
        return !windows.isEmpty();
    }

    @Nullable
    public IUIWindow getTop() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            IUIWindow window = windows.get(i);
            if (window.isVisible()) return window;
        }
        return null;
    }

    public List<IUIWindow> getWindows() {
        return Collections.unmodifiableList(windows);
    }

    public void bringToFront(IUIWindow window) {
        if (!windows.contains(window)) return;
        List<IUIWindow> group = withDescendants(window);
        windows.removeAll(group);
        windows.addAll(group);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        enter();
        try {
            for (IUIWindow window : windows) {
                if (!window.isVisible()) continue;
                graphics.enableScissor(window.getX(), window.getY(), window.getRight(), window.getBottom());
                try {
                    window.render(graphics, mouseX, mouseY, partialTick);
                } finally {
                    graphics.disableScissor();
                }
            }
        } finally {
            exit();
        }
    }

    public void mouseMoved(double mouseX, double mouseY) {
        enter();
        try {
            for (IUIWindow window : windows) {
                if (window.isVisible()) window.mouseMoved(mouseX, mouseY);
            }
        } finally {
            exit();
        }
    }

    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        enter();
        try {
            for (int i = windows.size() - 1; i >= 0; i--) {
                IUIWindow window = windows.get(i);
                if (!window.isVisible() || !window.isMouseOver(mouseX, mouseY)) continue;
                if (!window.isMouseClicked(mouseX, mouseY, button)) continue;
                bringToFront(window);
                if (button == 0) dragging = window;
                return true;
            }
            return false;
        } finally {
            exit();
        }
    }

    public boolean isMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        enter();
        try {
            IUIWindow captured = button == 0 ? dragging : null;
            if (captured != null) return captured.isMouseDragged(mouseX, mouseY, button, dragX, dragY);
            for (int i = windows.size() - 1; i >= 0; i--) {
                IUIWindow window = windows.get(i);
                if (window.isVisible() && window.isMouseOver(mouseX, mouseY)
                        && window.isMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
            }
            return false;
        } finally {
            exit();
        }
    }

    public boolean isMouseReleased(double mouseX, double mouseY, int button) {
        enter();
        try {
            IUIWindow captured = button == 0 ? dragging : null;
            if (button == 0) dragging = null;
            if (captured != null) return captured.isMouseReleased(mouseX, mouseY, button);
            for (int i = windows.size() - 1; i >= 0; i--) {
                IUIWindow window = windows.get(i);
                if (window.isVisible() && window.isMouseOver(mouseX, mouseY)
                        && window.isMouseReleased(mouseX, mouseY, button)) return true;
            }
            return false;
        } finally {
            exit();
        }
    }

    public boolean isMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        enter();
        try {
            for (int i = windows.size() - 1; i >= 0; i--) {
                IUIWindow window = windows.get(i);
                if (window.isVisible() && window.isMouseOver(mouseX, mouseY)
                        && window.isMouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
            }
            return false;
        } finally {
            exit();
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        enter();
        try {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (windows.isEmpty()) return false;
                closeTop();
                return true;
            }
            IUIWindow top = getTop();
            return top != null && top.keyPressed(keyCode, scanCode, modifiers);
        } finally {
            exit();
        }
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        enter();
        try {
            IUIWindow top = getTop();
            return top != null && top.keyReleased(keyCode, scanCode, modifiers);
        } finally {
            exit();
        }
    }

    public boolean charTyped(char codePoint, int modifiers) {
        enter();
        try {
            IUIWindow top = getTop();
            return top != null && top.charTyped(codePoint, modifiers);
        } finally {
            exit();
        }
    }

    private void enter() {
        dispatchDepth++;
    }

    private void exit() {
        dispatchDepth--;
        if (dispatchDepth == 0) flush();
    }

    private void flush() {
        if (!pendingAdd.isEmpty()) {
            List<IUIWindow> adds = new ArrayList<>(pendingAdd);
            pendingAdd.clear();
            for (IUIWindow window : adds) {
                doOpen(window);
            }
        }
        if (!pendingRemove.isEmpty()) {
            List<IUIWindow> removes = new ArrayList<>(pendingRemove);
            pendingRemove.clear();
            for (IUIWindow window : removes) {
                doClose(window);
            }
        }
    }

    private void doOpen(IUIWindow window) {
        windows.add(window);
        if (sized) {
            window.init(mc, screenWidth, screenHeight);
            clampInto(window);
        } else {
            needsInit.add(window);
        }
    }

    private void doClose(IUIWindow window) {
        List<IUIWindow> doomed = withDescendants(window);
        for (int i = doomed.size() - 1; i >= 0; i--) {
            IUIWindow closing = doomed.get(i);
            needsInit.remove(closing);
            pendingAdd.remove(closing);
            if (dragging == closing) dragging = null;
            if (windows.remove(closing)) closing.onClose();
        }
    }

    private List<IUIWindow> withDescendants(IUIWindow root) {
        List<IUIWindow> result = new ArrayList<>();
        result.add(root);
        for (int i = 0; i < result.size(); i++) {
            IUIWindow parent = result.get(i);
            for (IUIWindow candidate : windows) {
                if (candidate.getParent() == parent && !result.contains(candidate)) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    private List<IUIWindow> detachAll() {
        List<IUIWindow> doomed = new ArrayList<>(windows);
        windows.clear();
        pendingAdd.clear();
        pendingRemove.clear();
        needsInit.clear();
        dragging = null;
        return doomed;
    }

    private void updateSize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        sized = width > 0 && height > 0;
    }

    private void flushNeedsInit() {
        if (needsInit.isEmpty()) return;
        List<IUIWindow> waiting = new ArrayList<>(needsInit);
        needsInit.clear();
        if (!sized) {
            needsInit.addAll(waiting);
            return;
        }
        for (IUIWindow window : waiting) {
            if (!windows.contains(window)) continue;
            window.init(mc, screenWidth, screenHeight);
            clampInto(window);
        }
    }

    private void clampInto(IUIWindow window) {
        if (!sized) return;
        int x = Math.clamp((long) window.getX(), 0, Math.max(0, screenWidth - window.getWidth()));
        int y = Math.clamp((long) window.getY(), 0, Math.max(0, screenHeight - window.getHeight()));
        if (x != window.getX() || y != window.getY()) window.setPosition(x, y);
    }
}
