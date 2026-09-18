package org.moshang.tempusetchaos.registry;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.TempusEtChaos;

public class TECCapabilities {
    public static final BlockCapability<IFluidHandler, @Nullable Direction> FLUID_ENTROPY =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "chronon"), IFluidHandler.class);

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(FLUID_ENTROPY, TECBlockEntities.ENTROPY_NODE_BE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(FLUID_ENTROPY, TECBlockEntities.ENTROPY_REACTOR_BE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(FLUID_ENTROPY, TECBlockEntities.ENTROPY_PIPE_BE.get(), (be, side) -> be.getFluidHandler());

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TECBlockEntities.ENTROPY_REACTOR_BE.get(), (be, side) -> be.getItemHandler());
    }
}
