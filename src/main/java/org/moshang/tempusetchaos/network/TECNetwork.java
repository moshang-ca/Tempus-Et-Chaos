package org.moshang.tempusetchaos.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.moshang.tempusetchaos.TempusEtChaos;

@EventBusSubscriber(modid = TempusEtChaos.MODID)
public class TECNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                ChrononNetworkSyncPayload.TYPE,
                ChrononNetworkSyncPayload.STREAM_CODEC,
                ChrononNetworkSyncPayload.ClientCache::handle
        );
    }
}
