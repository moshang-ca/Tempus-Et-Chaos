package org.moshang.tempusetchaos;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.moshang.tempusetchaos.client.ChrononCableGeometryLoader;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECBlocks;
import org.moshang.tempusetchaos.registry.TECItems;
import org.slf4j.Logger;

@Mod(TempusEtChaos.MODID)
public class TempusEtChaos {
    public static final String MODID = "tempusetchaos";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TempusEtChaos(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onModelLoaderRegister);

        TECBlocks.BLOCK_DR.register(modEventBus);
        TECItems.ITEM_DR.register(modEventBus);
        TECBlockEntities.BE_TYPE_DR.register(modEventBus);
    }

    public void onModelLoaderRegister(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(MODID, "chronon_cable"), new ChrononCableGeometryLoader());
    }
}
