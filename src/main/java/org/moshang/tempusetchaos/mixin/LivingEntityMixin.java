package org.moshang.tempusetchaos.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private static final ResourceLocation TempusEtChaos$SLOWDOWN = ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "chronon_slowdown");

    @Unique
    private int TempusEtChaos$slowdownFactor = 1;
    @Unique
    private int TempusEtChaos$remainingTicks = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    public void TempusEtChaos$syncSlowdownData(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.level().isClientSide) return;
        CompoundTag data = self.getPersistentData();
        AttributeInstance speed = self.getAttribute(Attributes.MOVEMENT_SPEED);
        if (self.tickCount % 40 == 0) {
            if (data.contains("chronon_slowdown") && self.level().getGameTime() - data.getLong("chronon_slowdown_ts") <= 45) {
                TempusEtChaos$slowdownFactor = data.getInt("chronon_slowdown");
                TempusEtChaos$remainingTicks = 45;
                if (speed != null && !speed.hasModifier(TempusEtChaos$SLOWDOWN)) {
                    speed.addTransientModifier(
                            new AttributeModifier(TempusEtChaos$SLOWDOWN, 1f / TempusEtChaos$slowdownFactor - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            }
        }
        if (TempusEtChaos$remainingTicks > 0) {
            TempusEtChaos$remainingTicks--;
        }
        if (TempusEtChaos$remainingTicks == 0) {
            TempusEtChaos$slowdownFactor = 1;
            TempusEtChaos$remainingTicks = -1;
            data.remove("chronon_slowdown");
            data.remove("chronon_slowdown_ts");
            if (speed != null) speed.removeModifier(TempusEtChaos$SLOWDOWN);
        }
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    public void TempusEtChaos$onServerAiStep(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        int factor = TempusEtChaos$slowdownFactor;
        if (self.tickCount % factor != 0)
            ci.cancel();
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void TempusEtChaos$slowdownPhysMovement(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (TempusEtChaos$slowdownFactor > 1) {
            double factor = 1. / TempusEtChaos$slowdownFactor;
            self.setDeltaMovement(self.getDeltaMovement().multiply(factor, factor, factor));
        }
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V"))
    private void TempusEtChaos$slowdownTravelInput(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (TempusEtChaos$slowdownFactor > 1) {
            float factor = 1f / TempusEtChaos$slowdownFactor;
            self.xxa *= factor;
            self.zza *= factor;
            self.yya *= factor;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void saveAdditional(CompoundTag compound, CallbackInfo ci) {
        compound.putInt("chronon_remaining_ticks", TempusEtChaos$remainingTicks);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void readAdditional(CompoundTag compound, CallbackInfo ci) {
        TempusEtChaos$remainingTicks = compound.getInt("chronon_remaining_ticks");
    }
}
