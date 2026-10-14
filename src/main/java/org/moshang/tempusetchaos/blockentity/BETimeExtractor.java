package org.moshang.tempusetchaos.blockentity;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.tempusetchaos.api.RecipeableChrononNodeBE;
import org.moshang.tempusetchaos.api.storage.IO;
import org.moshang.tempusetchaos.api.storage.NotifiedItemStackHandler;
import org.moshang.tempusetchaos.recipe.TimeExtractorRecipe;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BETimeExtractor extends RecipeableChrononNodeBE<SingleRecipeInput, TimeExtractorRecipe> {
    public static final Set<Block> PRODUCE_CONDITIONS = Set.of(Blocks.BEDROCK, Blocks.DEEPSLATE, Blocks.END_STONE);

    @Getter
    private final NotifiedItemStackHandler inputHandler = new NotifiedItemStackHandler(2, IO.INPUT)
            .setOnContentsChanged(() -> { recipeDirty = true; setChanged(); });
    private final int extractSpeed = 10;     // 10 ch/tick

    @Setter
    private boolean canPassiveProduce = false;

    public BETimeExtractor(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.TIME_EXTRACTOR_BE.get(), pos, blockState,
                1200, TECRecipes.TIME_EXTRACTOR_RT.get());
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (innerNetwork != null && canPassiveProduce && !recipeRunning) {
            innerNetwork.receiveChronon(extractSpeed, false);
        }
    }

    @Override
    protected List<SingleRecipeInput> buildInputs() {
        List<SingleRecipeInput> inputs = new ArrayList<>(inputHandler.getSlots());
        for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
            inputs.add(new SingleRecipeInput(inputHandler.getStackInSlot(slot)));
        }
        return inputs;
    }

    @Override
    protected boolean onRecipeStart(TimeExtractorRecipe recipe) {
        return consume(List.of(recipe.getInputItem()), inputHandler);
    }

    @Override
    protected boolean produce(TimeExtractorRecipe recipe, boolean finishing) {
        int payout = recipe.chrononPerTick() + (finishing ? recipe.chrononOnFinish() : 0);
        if (innerNetwork == null || innerNetwork.receiveChronon(payout, true) < payout) return false;
        innerNetwork.receiveChronon(payout, false);
        return true;
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
        this.canPassiveProduce = tag.getBoolean("canPassiveProduce");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("canPassiveProduce", canPassiveProduce);
    }
}
