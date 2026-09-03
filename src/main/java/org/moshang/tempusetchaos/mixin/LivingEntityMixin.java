package org.moshang.tempusetchaos.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private int TempusEtChaos$slowdownFactor = 1;

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    public void TempusEtChaos$onServerAiStep(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        CompoundTag data = self.getPersistentData();
        if (data.contains("chronon_slowdown")) {
            int factor = data.getInt("chronon_slowdown");
            if (self.tickCount % factor != 0)
                ci.cancel();
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    public void TempusEtChaos$onAiStepHead(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        CompoundTag data = self.getPersistentData();
        if (data.contains("chronon_slowdown")) {
            TempusEtChaos$slowdownFactor  = data.getInt("chronon_slowdown");
        } else {
            TempusEtChaos$slowdownFactor = 1;
        }
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
}
