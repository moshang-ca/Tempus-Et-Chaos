package org.moshang.tempusetchaos.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.blockentity.BEAccelerator;
import org.moshang.tempusetchaos.blockentity.BEReducer;
import org.moshang.tempusetchaos.blockentity.BETimeExtractor;
import org.moshang.tempusetchaos.blockentity.BEVirtualNode;

import java.util.function.Supplier;

public class TECBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BE_TYPE_DR =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TempusEtChaos.MODID);

    public static final Supplier<BlockEntityType<BEVirtualNode>> VIRTUAL_NODE_BE =
            BE_TYPE_DR.register("virtual_node", () -> BlockEntityType.Builder.of(BEVirtualNode::new, TECBlocks.VIRTUAL_NODE.get()).build(null));
    public static final Supplier<BlockEntityType<BETimeExtractor>> TIME_EXTRACTOR_BE =
            BE_TYPE_DR.register("time_extractor", () -> BlockEntityType.Builder.of(BETimeExtractor::new, TECBlocks.TIME_EXTRACTOR.get()).build(null));
    public static final Supplier<BlockEntityType<BEAccelerator>> ACCELERATOR_BE =
            BE_TYPE_DR.register("accelerator", () -> BlockEntityType.Builder.of(BEAccelerator::new, TECBlocks.ACCELERATOR.get()).build(null));
    public static final Supplier<BlockEntityType<BEReducer>> REDUCER_BE =
            BE_TYPE_DR.register("reducer", () -> BlockEntityType.Builder.of(BEReducer::new, TECBlocks.REDUCER.get()).build(null));
}
