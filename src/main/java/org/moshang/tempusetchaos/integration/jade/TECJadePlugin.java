package org.moshang.tempusetchaos.integration.jade;

import org.moshang.tempusetchaos.block.BlockEntropyNode;
import org.moshang.tempusetchaos.block.BlockEntropyPipe;
import org.moshang.tempusetchaos.block.BlockEntropyReactor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class TECJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(TECServerDataProvider.INSTANCE, BlockEntropyNode.class);
        registration.registerBlockDataProvider(TECServerDataProvider.INSTANCE, BlockEntropyReactor.class);
        registration.registerBlockDataProvider(TECServerDataProvider.INSTANCE, BlockEntropyPipe.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TECComponentProvider.INSTANCE, BlockEntropyNode.class);
        registration.registerBlockComponent(TECComponentProvider.INSTANCE, BlockEntropyReactor.class);
        registration.registerBlockComponent(TECComponentProvider.INSTANCE, BlockEntropyPipe.class);
    }
}
