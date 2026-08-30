package org.moshang.tempusetchaos.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;

public class TECBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BE_TYPE_DR =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TempusEtChaos.MODID);
}
