package org.moshang.tempusetchaos.client.event;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.client.ClientFluidExtension;
import org.moshang.tempusetchaos.client.gui.AcceleratorScreen;
import org.moshang.tempusetchaos.client.model.BEVesselRenderer;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECFluids;
import org.moshang.tempusetchaos.registry.TECMenus;

@EventBusSubscriber(modid = TempusEtChaos.MODID, value = Dist.CLIENT)
public class TempusEtChaosClient {
    @SubscribeEvent
    public static void registerFluidClientExtension(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                ClientFluidExtension.fromFluid(TECFluids.GAS_ENTROPY_SOURCE.get(), 0xFFFFFFFF),
                TECFluids.GAS_ENTROPY_TYPE
        );
        ItemBlockRenderTypes.setRenderLayer(TECFluids.GAS_ENTROPY_SOURCE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(TECFluids.GAS_ENTROPY_FLOWING.get(), RenderType.translucent());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TECBlockEntities.ENTROPY_VESSEL_BE.get(), BEVesselRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TECMenus.ACCELERATOR_MENU.get(), AcceleratorScreen::new);
    }
}
