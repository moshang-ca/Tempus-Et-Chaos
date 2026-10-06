package org.moshang.tempusetchaos.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.menu.MenuAccelerator;

public class TECMenus {
    public static final DeferredRegister<MenuType<?>> MENU_DR =
            DeferredRegister.create(BuiltInRegistries.MENU, TempusEtChaos.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MenuAccelerator>> ACCELERATOR =
            MENU_DR.register("accelerator", () -> IMenuTypeExtension.create(
                    (containerId, inventory, buf) ->
                            new MenuAccelerator(containerId, inventory, buf.readBlockPos())
            ));
}
