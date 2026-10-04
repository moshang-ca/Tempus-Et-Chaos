package org.moshang.tempusetchaos.integration.jade;

import org.moshang.tempusetchaos.api.BaseChrononMachineryBlock;
import org.moshang.tempusetchaos.block.*;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class TECJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerFluidStorage(TECFluidStorageProvider.INSTANCE, BlockEntropyNode.class);
        registration.registerFluidStorage(TECFluidStorageProvider.INSTANCE, BlockEntropyReactor.class);
        registration.registerFluidStorage(TECFluidStorageProvider.INSTANCE, BlockEntropyPipe.class);
        registration.registerFluidStorage(TECFluidStorageProvider.INSTANCE, BlockEntropyVessel.class);
        registration.registerFluidStorage(TECFluidStorageProvider.INSTANCE, BlockEntropyForge.class);

        registration.registerBlockDataProvider(ChrononNetDataProvider.INSTANCE, BaseChrononMachineryBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerFluidStorageClient(TECFluidStorageProvider.INSTANCE);

        registration.registerBlockComponent(ChrononComponentProvider.INSTANCE, BaseChrononMachineryBlock.class);
    }
}
