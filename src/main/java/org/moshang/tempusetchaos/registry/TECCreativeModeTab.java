package org.moshang.tempusetchaos.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;

public class TECCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> CMT_DR = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TempusEtChaos.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CMT_DR.register("tec_main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tempusetchaos.main_tab"))
                    .icon(() -> new ItemStack(TECItems.ENTROPY_CRYSTAL.get()))
                    .displayItems(TECCreativeModeTab::addToMainTab)
                    .build());

    private static void addToMainTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(TECItems.ENTROPY_COOLING_MODULE);
        output.accept(TECItems.ENTROPY_CRYSTAL);
        output.accept(TECItems.GAS_ENTROPY_BUCKET);
        output.accept(TECItems.WRENCH);

        TECItems.BLOCK_ITEMS.values().forEach(output::accept);
    }
}
