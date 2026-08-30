package org.moshang.tempusetchaos.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.TempusEtChaos;

import java.util.HashMap;
import java.util.Map;

public class TECItems {
    public static final DeferredRegister.Items ITEM_DR = DeferredRegister.createItems(TempusEtChaos.MODID);
    public static final Map<String, DeferredItem<BlockItem>> BLOCK_ITEMS = new HashMap<>();

    public static void registerBlockItem(DeferredBlock<Block> block, @Nullable Item.Properties properties)
    {
        BLOCK_ITEMS.put(block.getRegisteredName(), ITEM_DR.registerSimpleBlockItem(block, properties != null ? properties : new Item.Properties()));
    }

    @Nullable
    public static BlockItem getBlockItem(String name) {
        DeferredItem<BlockItem> toReturn = BLOCK_ITEMS.get(name);
        return toReturn == null ? null : toReturn.get();
    }
}
