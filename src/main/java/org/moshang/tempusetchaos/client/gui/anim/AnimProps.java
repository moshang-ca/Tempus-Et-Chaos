package org.moshang.tempusetchaos.client.gui.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public final class AnimProps {
    private static final float EPSILON = 0.001f;
    private static final AnimProps DEFAULTS = new AnimProps();

    public float offsetX;
    public float offsetY;
    public float alpha = 1f;
    public float scaleX = 1f;
    public float scaleY = 1f;
    public float rotation;
    public float pivotX = 0.5f;
    public float pivotY = 0.5f;

    public void reset() {
        offsetX = DEFAULTS.offsetX;
        offsetY = DEFAULTS.offsetY;
        alpha = DEFAULTS.alpha;
        scaleX = DEFAULTS.scaleX;
        scaleY = DEFAULTS.scaleY;
        rotation = DEFAULTS.rotation;
        pivotX = DEFAULTS.pivotX;
        pivotY = DEFAULTS.pivotY;
    }

    public boolean isVisible() {
        return alpha > EPSILON;
    }

    public boolean isIdentity() {
        return offsetX == 0f && offsetY == 0f && scaleX == 1f && scaleY == 1f && rotation == 0f;
    }

    public void apply(PoseStack pose, int x, int y, int width, int height) {
        float px = width * pivotX;
        float py = height * pivotY;
        pose.translate(x + offsetX, y + offsetY, 0f);
        pose.translate(px, py, 0f);
        if (rotation != 0f) pose.mulPose(Axis.ZP.rotation(rotation));
        if (scaleX != 1f || scaleY != 1f) pose.scale(scaleX, scaleY, 1f);
        pose.translate(-px, -py, 0f);
    }

    /** Host space point -> the node's own space, origin at its top left. */
    public Local toLocal(double screenX, double screenY, int x, int y, int width, int height) {
        float px = width * pivotX;
        float py = height * pivotY;
        double sx = Math.abs(scaleX) < EPSILON ? EPSILON : scaleX;
        double sy = Math.abs(scaleY) < EPSILON ? EPSILON : scaleY;
        double dx = screenX - x - offsetX - px;
        double dy = screenY - y - offsetY - py;
        double c = Math.cos(rotation);
        double s = Math.sin(rotation);
        // apply() scales then rotates, so its inverse rotates first and only then divides
        double ux = dx * c + dy * s;
        double uy = -dx * s + dy * c;
        return new Local(px + ux / sx, py + uy / sy);
    }

    /** AABB of the transformed node, in host space. */
    public Rect clip(int x, int y, int width, int height) {
        if (isIdentity()) return new Rect(x, y, x + width, y + height);

        float px = width * pivotX;
        float py = height * pivotY;
        float c = (float) Math.cos(rotation);
        float s = (float) Math.sin(rotation);
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < 4; i++) {
            float dx = ((i & 1) == 0 ? 0f : width) - px;
            float dy = ((i & 2) == 0 ? 0f : height) - py;
            float fx = x + offsetX + px + dx * scaleX * c - dy * scaleY * s;
            float fy = y + offsetY + py + dx * scaleX * s + dy * scaleY * c;
            minX = Math.min(minX, fx);
            minY = Math.min(minY, fy);
            maxX = Math.max(maxX, fx);
            maxY = Math.max(maxY, fy);
        }
        return new Rect(Math.round(minX), Math.round(minY), Math.round(maxX), Math.round(maxY));
    }

    public record Local(double x, double y) { }

    public record Rect(int left, int top, int right, int bottom) { }
}
