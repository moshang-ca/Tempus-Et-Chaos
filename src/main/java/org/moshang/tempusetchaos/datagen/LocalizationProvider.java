package org.moshang.tempusetchaos.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.moshang.tempusetchaos.registry.TECBlocks;
import org.moshang.tempusetchaos.registry.TECItems;

public class LocalizationProvider extends LanguageProvider {
    public LocalizationProvider(PackOutput output, String modid, String locale) {
        super(output, modid, locale);
    }

    @Override
    protected void addTranslations() {
        this.add(TECBlocks.ACCELERATOR.get(), toDisplayName(TECBlocks.ACCELERATOR));
        this.add(TECBlocks.CHRONON_NET_CABLE.get(), toDisplayName(TECBlocks.CHRONON_NET_CABLE));
        this.add(TECBlocks.ENTROPY_CRYSTAL_BLOCK.get(), toDisplayName(TECBlocks.ENTROPY_CRYSTAL_BLOCK));
        this.add(TECBlocks.ENTROPY_NODE.get(), toDisplayName(TECBlocks.ENTROPY_NODE));
        this.add(TECBlocks.ENTROPY_PIPE.get(), toDisplayName(TECBlocks.ENTROPY_PIPE));
        this.add(TECBlocks.ENTROPY_REACTOR.get(), toDisplayName(TECBlocks.ENTROPY_REACTOR));
        this.add(TECBlocks.GAS_ENTROPY.get(), toDisplayName(TECBlocks.GAS_ENTROPY));
        this.add(TECBlocks.REDUCER.get(), toDisplayName(TECBlocks.REDUCER));
        this.add(TECBlocks.TIME_EXTRACTOR.get(), toDisplayName(TECBlocks.TIME_EXTRACTOR));

        this.add(TECItems.ENTROPY_CRYSTAL.get(), toDisplayName(TECItems.ENTROPY_CRYSTAL));
        this.add(TECItems.GAS_ENTROPY_BUCKET.get(), toDisplayName(TECItems.GAS_ENTROPY_BUCKET));
        this.add(TECItems.WRENCH.get(), toDisplayName(TECItems.WRENCH));
    }

    protected static <R, T extends R> String toDisplayName(DeferredHolder<R, T> registerHolder) {
        String[] words = registerHolder.getRegisteredName().split(":")[1].split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }
        return sb.toString().trim();
    }
}
