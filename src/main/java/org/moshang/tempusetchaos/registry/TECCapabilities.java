package org.moshang.tempusetchaos.registry;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.api.IWrench;
import org.moshang.tempusetchaos.api.IWrenchable;

public class TECCapabilities {
    public static final BlockCapability<IFluidHandler, @Nullable Direction> FLUID_ENTROPY =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "entropy"), IFluidHandler.class);
    public static final BlockCapability<IWrenchable, @Nullable Void> WRENCHABLE =
            BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "wrenchable"), IWrenchable.class);

    public static final ItemCapability<IWrench, @Nullable Void> WRENCH =
            ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "wrench"), IWrench.class);

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(FLUID_ENTROPY, TECBlockEntities.ENTROPY_NODE_BE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(FLUID_ENTROPY, TECBlockEntities.ENTROPY_REACTOR_BE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(FLUID_ENTROPY, TECBlockEntities.ENTROPY_PIPE_BE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(FLUID_ENTROPY, TECBlockEntities.ENTROPY_VESSEL_BE.get(), (be, side) -> be.getFluidHandler());

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TECBlockEntities.ENTROPY_REACTOR_BE.get(), (be, side) -> be.getItemHandler());

        event.registerItem(WRENCH, (stack, context) -> new IWrench.WrenchHandler(), TECItems.WRENCH.get());

        event.registerBlock(
                WRENCHABLE, (lvl, p, st, be, ctx) -> st.getBlock() instanceof IWrenchable wrenchable ? wrenchable : null,
                TECBlocks.ACCELERATOR.get(), TECBlocks.ENTROPY_NODE.get(), TECBlocks.ENTROPY_REACTOR.get(), TECBlocks.REDUCER.get(), TECBlocks.TIME_EXTRACTOR.get(),
                TECBlocks.ENTROPY_PIPE.get()
        );
    }
}
