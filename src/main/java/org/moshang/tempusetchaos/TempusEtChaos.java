package org.moshang.tempusetchaos;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TempusEtChaos.MODID)
public class TempusEtChaos {
    public static final String MODID = "tempusetchaos";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TempusEtChaos(IEventBus modEventBus, ModContainer modContainer) {
    }
}
