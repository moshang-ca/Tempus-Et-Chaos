package org.moshang.tempusetchaos.blockentity;

import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.registry.TECBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
public class BETimeExtractor extends BaseChrononNodeBlockEntity {
    public static final Set<Block> PRODUCE_CONDITIONS = Set.of(Blocks.BEDROCK, Blocks.DEEPSLATE, Blocks.END_STONE);

    private final int extractSpeed = 1;     // 1 ch/tick
    @Setter
    private boolean canProduce = false;

    public BETimeExtractor(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.TIME_EXTRACTOR_BE.get(), pos, blockState, 1200);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (innerNetwork != null) {
            if (canProduce) {
                innerNetwork.receiveChronon(extractSpeed, false);
                System.out.println("BETimeExtractor received time extraction speed: " + extractSpeed);
            }
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PRODUCER;
    }

    @Override
    public int getProduced() {
        return extractSpeed;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.canProduce = tag.getBoolean("canProduce");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("canProduce", canProduce);
    }
}
