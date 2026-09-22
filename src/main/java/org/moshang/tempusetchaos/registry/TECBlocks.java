package org.moshang.tempusetchaos.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.api.VesselTier;
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
    public static final DeferredBlock<BlockEntropyReactor> ENTROPY_REACTOR =
            registerItemLikeBlock("entropy_reactor", BlockEntropyReactor::new);
    public static final DeferredBlock<BlockEntropyNode> ENTROPY_NODE =
            registerItemLikeBlock("entropy_node", BlockEntropyNode::new);
    public static final DeferredBlock<BlockEntropyPipe> ENTROPY_PIPE =
            registerItemLikeBlock("entropy_pipe", BlockEntropyPipe::new);
    public static final DeferredBlock<BlockEntropyVessel> IRON_ENTROPY_VESSEL =
            registerItemLikeBlock("iron_entropy_vessel", (properties) -> new BlockEntropyVessel(properties, VesselTier.IRON));
    public static final DeferredBlock<BlockEntropyVessel> REINFORCE_ENTROPY_VESSEL =
            registerItemLikeBlock("reinfroce_entropy_vessel", (properties) -> new BlockEntropyVessel(properties, VesselTier.REINFORCED));
    public static final DeferredBlock<BlockEntropyVessel> OBSIDIAN_ENTROPY_VESSEL =
            registerItemLikeBlock("obsidian_entropy_vessel", (properties) -> new BlockEntropyVessel(properties, VesselTier.OBSIDIAN));
    public static final DeferredBlock<BlockEntropyVessel> NETHERITE_ENTROPY_VESSEL =
            registerItemLikeBlock("netherite_entropy_vessel", (properties) -> new BlockEntropyVessel(properties, VesselTier.NETHERITE));
    public static final DeferredBlock<BlockEntropyVessel> CREATIVE_ENTROPY_VESSEL =
            registerItemLikeBlock("creative_entropy_vessel", (properties) -> new BlockEntropyVessel(properties, VesselTier.CREATIVE));

    public static final DeferredBlock<LiquidBlock> GAS_ENTROPY =
            BLOCK_DR.register("gas_entropy_block", () -> new LiquidBlock(TECUtilities.GAS_ENTROPY_SOURCE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));

    public static final DeferredBlock<Block> ENTROPY_CRYSTAL_BLOCK =
            registerItemLikeSimpleBlock("entropy_crystal_block");

    private static <T extends Block> DeferredBlock<T> registerItemLikeBlock(String name, Function<BlockBehaviour.Properties, ? extends T> func) {
        DeferredBlock<T> toReturn = BLOCK_DR.registerBlock(name, func);
        TECItems.registerBlockItem(toReturn, null);
        return toReturn;
    }

    private static DeferredBlock<Block> registerItemLikeSimpleBlock(String name) {
        return registerItemLikeBlock(name, Block::new);
    }


}
