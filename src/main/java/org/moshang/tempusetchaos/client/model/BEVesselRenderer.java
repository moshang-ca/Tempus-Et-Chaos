package org.moshang.tempusetchaos.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;
import org.moshang.tempusetchaos.blockentity.BEEntropyVessel;
import org.moshang.tempusetchaos.registry.TECItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BEVesselRenderer implements BlockEntityRenderer<BEEntropyVessel> {
    private static final float WALL_MIN = 3f / 16f;
    private static final float WALL_MAX = 13f / 16f;
    private static final float WIN_BOTTOM = 3f / 16f;
    private static final float WIN_TOP = 11f / 16f;
    private static final float FLUID_GAP = 1f / 16f;

    private final ItemRenderer moduleRenderer;
    private final ItemStack moduleStack;

    public BEVesselRenderer(BlockEntityRendererProvider.Context context) {
        this.moduleRenderer = context.getItemRenderer();
        moduleStack = new ItemStack(TECItems.ENTROPY_COOLING_MODULE.get());
    }

    @Override
    public void render(BEEntropyVessel be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Matrix4f pose = poseStack.last().pose();
        FluidStack stack = be.getFluidHandler().getFluid();
        if (!stack.isEmpty()) {
            int capacity = be.getFluidHandler().getCapacity();
            if (capacity > 0) {
                float ratio = Math.min(1f, (float) stack.getAmount() / capacity);
                if (ratio > 0) renderWindow(pose, bufferSource, packedLight, stack, ratio);
            }
        }

        for (int i = 0; i < 4; i++) {
            if (be.getCoolingDuration(i) > 0) {
                poseStack.pushPose();
                poseStack.translate(.5, .5, .5);

                switch (Direction.from2DDataValue(i)) {
                    case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                    case WEST  -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                    case NORTH -> {}
                    case EAST  -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
                poseStack.translate(0, 0, -0.375);
                moduleRenderer.renderStatic(moduleStack, ItemDisplayContext.NONE,
                        packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), 0);

                poseStack.popPose();
            }
        }
    }

    private void renderWindow(Matrix4f pose, MultiBufferSource bufferSource, int light, FluidStack stack, float ratio) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(stack.getFluidType());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ext.getStillTexture(stack));
        int color = ext.getTintColor(stack);

        float lo = WALL_MIN + FLUID_GAP;
        float hi = WALL_MAX - FLUID_GAP;
        float surface = WIN_BOTTOM + (WIN_TOP - WIN_BOTTOM) * ratio;

        float half = (hi - lo) * 0.5f;
        float u0 = sprite.getU(0.5f - half);
        float u1 = sprite.getU(0.5f + half);
        float t0 = sprite.getV(0.5f - half);
        float t1 = sprite.getV(0.5f + half);
        float vBottom = sprite.getV(1f);
        float vSurface = sprite.getV(1f - (WIN_TOP - WIN_BOTTOM) * ratio);

        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        quad(vc, pose, color, light, lo, surface, lo, u0, vSurface, 0, 0, -1);
        quad(vc, pose, color, light, hi, surface, lo, u1, vSurface, 0, 0, -1);
        quad(vc, pose, color, light, hi, WIN_BOTTOM, lo, u1, vBottom, 0, 0, -1);
        quad(vc, pose, color, light, lo, WIN_BOTTOM, lo, u0, vBottom, 0, 0, -1);

        quad(vc, pose, color, light, lo, WIN_BOTTOM, hi, u0, vBottom, 0, 0, 1);
        quad(vc, pose, color, light, hi, WIN_BOTTOM, hi, u1, vBottom, 0, 0, 1);
        quad(vc, pose, color, light, hi, surface, hi, u1, vSurface, 0, 0, 1);
        quad(vc, pose, color, light, lo, surface, hi, u0, vSurface, 0, 0, 1);

        quad(vc, pose, color, light, hi, surface, lo, u0, vSurface, 1, 0, 0);
        quad(vc, pose, color, light, hi, surface, hi, u1, vSurface, 1, 0, 0);
        quad(vc, pose, color, light, hi, WIN_BOTTOM, hi, u1, vBottom, 1, 0, 0);
        quad(vc, pose, color, light, hi, WIN_BOTTOM, lo, u0, vBottom, 1, 0, 0);

        quad(vc, pose, color, light, lo, WIN_BOTTOM, lo, u0, vBottom, -1, 0, 0);
        quad(vc, pose, color, light, lo, WIN_BOTTOM, hi, u1, vBottom, -1, 0, 0);
        quad(vc, pose, color, light, lo, surface, hi, u1, vSurface, -1, 0, 0);
        quad(vc, pose, color, light, lo, surface, lo, u0, vSurface, -1, 0, 0);

        quad(vc, pose, color, light, lo, surface, lo, u0, t0, 0, 1, 0);
        quad(vc, pose, color, light, lo, surface, hi, u0, t1, 0, 1, 0);
        quad(vc, pose, color, light, hi, surface, hi, u1, t1, 0, 1, 0);
        quad(vc, pose, color, light, hi, surface, lo, u1, t0, 0, 1, 0);
    }

    private static void quad(VertexConsumer vc, Matrix4f pose, int color, int light,
                             float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 0xFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
