package org.moshang.tempusetchaos.registry;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.api.IChrononStorage;

public class TECCapabilities {
    public static final BlockCapability<IChrononStorage, @Nullable Direction> CHRONON =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "chronon"), IChrononStorage.class);

    public static void register(RegisterCapabilitiesEvent event) {
    }
}
