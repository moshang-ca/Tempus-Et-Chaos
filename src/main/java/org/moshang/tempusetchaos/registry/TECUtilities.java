package org.moshang.tempusetchaos.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.moshang.tempusetchaos.TempusEtChaos;

public class TECUtilities {
    public static final DeferredRegister<FluidType> FLUID_TYPE_DR = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, TempusEtChaos.MODID);
    public static final DeferredRegister<Fluid> FLUID_DR = DeferredRegister.create(BuiltInRegistries.FLUID, TempusEtChaos.MODID);


    public static final DeferredHolder<FluidType, FluidType> GAS_ENTROPY_TYPE =
            FLUID_TYPE_DR.register("gas_entropy_type", () -> new FluidType(
                    FluidType.Properties.create()
                            .descriptionId("fluid.tempusetchaos.gas_entropy")
                            .density(-1000)
                            .viscosity(200)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .canConvertToSource(true)
                            .canHydrate(true)
            ));

    public static final DeferredHolder<Fluid, FlowingFluid> GAS_ENTROPY_SOURCE =
            FLUID_DR.register("gas_entropy", () -> new BaseFlowingFluid.Source(FluidProperties.GAS_ENTROPY));
    public static final DeferredHolder<Fluid, FlowingFluid> GAS_ENTROPY_FLOWING =
            FLUID_DR.register("gas_entropy_flowing", () -> new BaseFlowingFluid.Flowing(FluidProperties.GAS_ENTROPY));

    public static class FluidProperties {
        public static final BaseFlowingFluid.Properties GAS_ENTROPY =
                new BaseFlowingFluid.Properties(GAS_ENTROPY_TYPE, GAS_ENTROPY_SOURCE, GAS_ENTROPY_FLOWING)
                        .block(TECBlocks.GAS_ENTROPY)
                        .bucket(TECItems.GAS_ENTROPY_BUCKET)
                        .slopeFindDistance(6)
                        .levelDecreasePerBlock(1);
    }
}
