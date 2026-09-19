package org.moshang.tempusetchaos.integration.jade;

import org.moshang.tempusetchaos.block.BlockEntropyNode;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class TECJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(TECServerDataProvider.INSTANCE, BlockEntropyNode.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TECComponentProvider.INSTANCE, BlockEntropyNode.class);
    }
}
