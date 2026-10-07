package org.moshang.tempusetchaos.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.data.ChrononNetworkData;

@EventBusSubscriber(modid = TempusEtChaos.MODID)
public class TempusEtChaosServer {
    @SubscribeEvent
    public static void onServerTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ChrononNetworkData data = ChrononNetworkData.get(level);
        if (data.isEmpty()) return;
        data.serverTick();
    }
}
