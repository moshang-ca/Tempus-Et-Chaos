package org.moshang.tempusetchaos.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.blockentity.network.PipeNetManager;

@EventBusSubscriber(modid = TempusEtChaos.MODID)
public class EntropyPipeTickHandler {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            PipeNetManager.get(serverLevel).tick();
        }
    }
}
