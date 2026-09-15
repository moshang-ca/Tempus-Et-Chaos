package org.moshang.tempusetchaos.client.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.client.ClientFluidExtension;
import org.moshang.tempusetchaos.registry.TECUtilities;

@EventBusSubscriber(modid = TempusEtChaos.MODID, value = Dist.CLIENT)
public class TempusEtChaosClient {
    @SubscribeEvent
    public static void registerFluidClientExtension(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                ClientFluidExtension.fromFluid(TECUtilities.GAS_ENTROPY_SOURCE.get(), 0xFF00FFFF),
                TECUtilities.GAS_ENTROPY_TYPE
        );
    }
}
