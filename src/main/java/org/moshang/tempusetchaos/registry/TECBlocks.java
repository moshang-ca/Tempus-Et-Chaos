package org.moshang.tempusetchaos.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.block.*;

import java.util.function.Function;

public class TECBlocks {
    public static final DeferredRegister.Blocks BLOCK_DR = DeferredRegister.createBlocks(TempusEtChaos.MODID);

    public static final DeferredBlock<BlockChrononNetCable> CHRONON_NET_CABLE =
            registerItemLikeBlock("chronon_cable", BlockChrononNetCable::new);
    public static final DeferredBlock<BlockVirtualNode> VIRTUAL_NODE =
            registerItemLikeBlock("virtual_node", BlockVirtualNode::new);
    public static final DeferredBlock<BlockTimeExtractor> TIME_EXTRACTOR =
            registerItemLikeBlock("time_extractor", BlockTimeExtractor::new);
    public static final DeferredBlock<BlockAccelerator> ACCELERATOR =
            registerItemLikeBlock("accelerator", BlockAccelerator::new);
    public static final DeferredBlock<BlockReducer> REDUCER =
            registerItemLikeBlock("reducer", BlockReducer::new);

    private static <T extends Block> DeferredBlock<T> registerItemLikeBlock(String name, Function<BlockBehaviour.Properties, ? extends T> func) {
        DeferredBlock<T> toReturn = BLOCK_DR.registerBlock(name, func);
        TECItems.registerBlockItem(toReturn, null);
        return toReturn;
    }


}
