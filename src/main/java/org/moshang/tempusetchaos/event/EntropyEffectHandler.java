package org.moshang.tempusetchaos.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.data.EntropyWorldData;

@EventBusSubscriber(modid = TempusEtChaos.MODID)
public class EntropyEffectHandler {
    @SubscribeEvent
    public static void ServerEntropyParticleSpawnSend(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 2 == 1) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();
            BlockPos playerPos = player.blockPosition();
            RandomSource rand = player.getRandom();

            float concentration = EntropyWorldData.getConcentration(level, player.chunkPosition());
            if (concentration <= 10f) continue;

            int particleCnt = (int) (concentration * 1.5);
            for (int i = 0; i < particleCnt; i++) {
                int localX = rand.nextIntBetweenInclusive(-8, 8) + playerPos.getX();
                int localZ = rand.nextIntBetweenInclusive(-8, 8) + playerPos.getZ();
                int y = rand.nextInt(5) + playerPos.getY();

                BlockPos particlePos = new BlockPos(localX, y, localZ);
                if (particlePos.distSqr(player.blockPosition()) > 64) continue;
                level.sendParticles(
                    player, ParticleTypes.PORTAL, false,
                    particlePos.getX() + .5f, particlePos.getY(), particlePos.getZ() + .5f,
                    1,
                    .2, .2, .2,
                    0.5
                );
            }
        }
    }

    @SubscribeEvent
    public static void ServerEntropyEntityEffect(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.tickCount % 40 == 0) {
            if ((entity instanceof LivingEntity living) && (entity.level() instanceof ServerLevel level)) {
                float concentration = EntropyWorldData.getConcentration(level, living.chunkPosition());
                if (concentration > 30f) {
                    float intensity = (concentration - 30) / 70f;
                    applyEffect(living, intensity);
                }
            }
        }
    }

    private static void applyEffect(LivingEntity living, float intensity) {
        living.hurt(living.damageSources().magic(), intensity * 3.5f + .5f);

        if (intensity > .1f) {
            int amplifier = (int) (intensity * 2);
            if (intensity > .2f)
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, amplifier));
            if (intensity > .5f)
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, amplifier));
        }
    }
}
