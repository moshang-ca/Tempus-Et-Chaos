package org.moshang.tempusetchaos.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.TempusEtChaos;

public class ClientFluidExtension implements IClientFluidTypeExtensions {
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final int tintColor;

    public static ClientFluidExtension fromFluid(Fluid fluid, int tintColor) {
        ResourceLocation fluidName = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidName.getPath().equals("empty")) {
            return new ClientFluidExtension(
                    ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "unknown"),
                    ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "unknown"),
                    tintColor
            );
        }
        String prefix = fluidName.getNamespace() + "block/";
        return new ClientFluidExtension(
                ResourceLocation.parse(prefix + fluidName.getPath() + "_still"),
                ResourceLocation.parse(prefix + fluidName.getPath() + "_flow"),
                tintColor
        );
    }

    public ClientFluidExtension(ResourceLocation flowingTexture, ResourceLocation stillTexture, int tintColor) {
        this.flowingTexture = flowingTexture;
        this.stillTexture = stillTexture;
        this.tintColor = tintColor;
    }

    @Override
    @NotNull
    public ResourceLocation getFlowingTexture() {
        return flowingTexture;
    }

    @Override
    @NotNull
    public ResourceLocation getStillTexture() {
        return stillTexture;
    }

    @Override
    public int getTintColor() {
        return tintColor;
    }
}
