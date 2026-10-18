package org.moshang.tempusetchaos.blockentity;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.RecipeableChrononNodeBE;
import org.moshang.tempusetchaos.api.storage.IO;
import org.moshang.tempusetchaos.api.storage.NotifiedItemStackHandler;
import org.moshang.tempusetchaos.menu.MenuExtractor;
import org.moshang.tempusetchaos.recipe.ProcessingRecipe;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BETimeExtractor extends RecipeableChrononNodeBE<ProcessingRecipe.TimeExtracting> implements MenuProvider {
    public static final Set<Block> PRODUCE_CONDITIONS = Set.of(Blocks.BEDROCK, Blocks.DEEPSLATE, Blocks.END_STONE);

    @Getter
    private final NotifiedItemStackHandler inputHandler = new NotifiedItemStackHandler(2, IO.INPUT)
            .setOnContentsChanged(() -> { recipeDirty = true; setChanged(); });
    private final int extractSpeed = 10;     // 10 ch/tick

    @Getter
    private boolean canPassiveProduce = false;

    public BETimeExtractor(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.TIME_EXTRACTOR_BE.get(), pos, blockState,
                1200, TECRecipes.TIME_EXTRACTOR_RT.get());
    }

    @Override
    protected List<NotifiedItemStackHandler> itemInputs() {
        return List.of(inputHandler);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (innerNetwork != null && canPassiveProduce && !recipeRunning) {
            innerNetwork.receiveChronon(extractSpeed, false);
        }
    }

    public void setCanPassiveProduce(boolean canPassiveProduce) {
        if (this.canPassiveProduce == canPassiveProduce) return;
        this.canPassiveProduce = canPassiveProduce;
        markUpdate();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PRODUCER;
    }

    @Override
    public int getProduced() {
        return canPassiveProduce ? extractSpeed : 0;
    }

    @Override
    public float getChrononRate() {
        return getProcessingTime() > 0 ? super.getChrononRate() : getProduced();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.canPassiveProduce = tag.getBoolean("canPassiveProduce");
        inputHandler.deserializeNBT(registries, tag.getCompound("inputs"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("canPassiveProduce", canPassiveProduce);
        tag.put("inputs", inputHandler.serializeNBT(registries));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.tempusetchaos.time_extractor");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MenuExtractor(containerId, playerInventory, this);
    }
}
