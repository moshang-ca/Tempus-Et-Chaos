package org.moshang.tempusetchaos.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;

import java.util.function.Function;

public class TECBlocks {
    public static final DeferredRegister.Blocks BLOCK_DR = DeferredRegister.createBlocks(TempusEtChaos.MODID);

    private static DeferredBlock<Block> registerItemLikeBlock(String name, Function<BlockBehaviour.Properties, ? extends Block> func) {
        DeferredBlock<Block> toReturn = BLOCK_DR.registerBlock(name, func);
        TECItems.registerBlockItem(toReturn, null);
        return toReturn;
    }
}
