package org.moshang.tempusetchaos.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.client.gui.AbstractWindowScreen;
import org.moshang.tempusetchaos.integration.jei.gui.GhostSlotHandler;
import org.moshang.tempusetchaos.integration.jei.gui.WindowScreenProperties;

import javax.annotation.ParametersAreNonnullByDefault;

@JeiPlugin
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TECJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(AbstractWindowScreen.class, new WindowScreenProperties());
        registration.addGhostIngredientHandler(AbstractWindowScreen.class, new GhostSlotHandler());
    }
}
