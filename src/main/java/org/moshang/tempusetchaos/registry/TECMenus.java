package org.moshang.tempusetchaos.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;

public class TECMenus {
    public static final DeferredRegister<MenuType<?>> MENU_DR =
            DeferredRegister.create(BuiltInRegistries.MENU, TempusEtChaos.MODID);
}
