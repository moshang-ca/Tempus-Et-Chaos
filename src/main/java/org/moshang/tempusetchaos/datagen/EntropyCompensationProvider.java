package org.moshang.tempusetchaos.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.data.EntropyCompensation;
import org.moshang.tempusetchaos.registry.TECBlocks;
import org.moshang.tempusetchaos.registry.TECItems;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EntropyCompensationProvider extends JsonCodecProvider<EntropyCompensation.Tier> {
    public EntropyCompensationProvider(PackOutput output,
                                       CompletableFuture<HolderLookup.Provider> lookupProvider,
                                       String modId, ExistingFileHelper existingFileHelper) {
        super(output, PackOutput.Target.DATA_PACK, EntropyCompensation.DIRECTORY, PackType.SERVER_DATA,
                EntropyCompensation.Tier.CODEC, lookupProvider, modId, existingFileHelper);
    }

    @Override
    protected void gather() {
        unconditional(
                TempusEtChaos.id("low"),
                new EntropyCompensation.Tier(0f, List.of(
                        new EntropyCompensation.Output(new ItemStack(TECItems.ENTROPY_CRYSTAL.get(), 2), 1f)
                ))
        );

        unconditional(
                TempusEtChaos.id("medium"),
                new EntropyCompensation.Tier(30f, List.of(
                        new EntropyCompensation.Output(new ItemStack(TECBlocks.ENTROPY_CRYSTAL_BLOCK), 0.5f)
                ))
        );
    }
}
