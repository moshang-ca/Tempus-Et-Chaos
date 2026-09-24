package org.moshang.tempusetchaos.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.moshang.tempusetchaos.blockentity.BEEntropyVessel;
import org.moshang.tempusetchaos.registry.TECItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BEVesselRenderer implements BlockEntityRenderer<BEEntropyVessel> {
    private final ItemRenderer moduleRenderer;


    public BEVesselRenderer(BlockEntityRendererProvider.Context context) {
        this.moduleRenderer = context.getItemRenderer();
    }

    @Override
    public void render(BEEntropyVessel be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack coolingModule = new ItemStack(TECItems.ENTROPY_COOLING_MODULE.get());

        for (int i = 0; i < 4; i++) {
            if (be.getCoolingDuration(i) > 0) {
                poseStack.pushPose();
                poseStack.translate(.5, .5, .5);

                switch (Direction.from2DDataValue(i)) {
                    case SOUTH: poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    case WEST: poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                    case NORTH: poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    case EAST: poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
                }
                poseStack.translate(0, 0, .5);
                moduleRenderer.renderStatic(coolingModule, ItemDisplayContext.FIXED,
                        packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), 0);

                poseStack.popPose();
            }
        }
    }
}
