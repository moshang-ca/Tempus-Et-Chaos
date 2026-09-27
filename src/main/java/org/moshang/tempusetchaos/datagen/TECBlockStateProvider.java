package org.moshang.tempusetchaos.datagen;

import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.moshang.tempusetchaos.registry.TECBlocks;

public class TECBlockStateProvider extends BlockStateProvider {
    public TECBlockStateProvider(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
        super(output, modid, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(TECBlocks.ENTROPY_CRYSTAL_ORE.get(), models().cubeAll("entropy_crystal_ore", modLoc("block/entropy_crystal_ore")));
        simpleBlockWithItem(TECBlocks.ENTROPY_CRYSTAL_BLOCK.get(), models().cubeAll("entropy_crystal_block", modLoc("block/entropy_crystal_block")));

        horizontalBlockWithItem(TECBlocks.ACCELERATOR, models().orientableWithBottom("accelerator",
                modLoc("block/accelerator/side"), modLoc("block/accelerator/front"), modLoc("block/accelerator/bottom"), modLoc("block/accelerator/top")));
        horizontalBlockWithItem(TECBlocks.ENTROPY_NODE, models().orientableWithBottom("entropy_node",
                modLoc("block/entropy_node/side"), modLoc("block/entropy_node/front"), modLoc("block/entropy_node/bottom"), modLoc("block/entropy_node/top")));
    }

    private void horizontalBlockWithItem(Holder<Block> block, ModelFile model) {
        horizontalBlock(block.value(), model);
        String path = block.unwrapKey().map(key -> key.location().getPath()).orElseThrow(() -> new IllegalStateException("Block is not registered: " + block));
        itemModels().withExistingParent(path, model.getLocation());
    }

}
