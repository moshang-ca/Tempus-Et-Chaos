package org.moshang.tempusetchaos.integration.jade;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.registry.TECCapabilities;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.util.JadeForgeUtils;

public enum TECFluidStorageProvider implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, FluidView> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "entropy_fluid");
    }

    @Override
    public @Nullable List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
        IFluidHandler handler = handlerOf(accessor);
        return handler == null ? null : JadeForgeUtils.fromFluidHandler(handler, accessor.nbtOps());
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return handlerOf(accessor) != null;
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
        return ClientViewGroup.map(groups, group -> FluidView.readDefault(group, accessor.nbtOps()), null);
    }

    private static IFluidHandler handlerOf(Accessor<?> accessor) {
        return accessor instanceof BlockAccessor block
                ? block.getLevel().getCapability(TECCapabilities.FLUID_ENTROPY, block.getPosition(), null)
                : null;
    }
}
