package org.moshang.tempusetchaos.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.blockentity.BEVirtualNode;

import java.util.function.Supplier;

public class TECBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BE_TYPE_DR =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TempusEtChaos.MODID);

    public static final Supplier<BlockEntityType<BEVirtualNode>> VIRTUAL_NODE_BE =
            BE_TYPE_DR.register("virtual_node", () -> BlockEntityType.Builder.of(BEVirtualNode::new, TECBlocks.VIRTUAL_NODE.get()).build(null));
}
