package org.moshang.tempusetchaos.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.data.EntropyWorldData;
import org.moshang.tempusetchaos.util.HashChain;

@EventBusSubscriber(modid = TempusEtChaos.MODID)
public class EntropyEffectHandler {
    private static final HashChain<Block> AGING_CHAINS = new HashChain<>(BuiltInRegistries.BLOCK::getId);

    static {
        AGING_CHAINS.addChain(Blocks.STONE, Blocks.COBBLESTONE, Blocks.GRAVEL, Blocks.SAND);
        AGING_CHAINS.addChain(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.SAND, Blocks.GLASS);
    }

    @SubscribeEvent
    public static void ServerEntropyParticleSpawnSend(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 10 != 0) return;

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
    public static void BlockAged(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 1200 != 0) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            EntropyWorldData entropyData = EntropyWorldData.get(level);
            for (ChunkPos pos : entropyData.getEntropiedChunks()) {
                int count = level.random.nextIntBetweenInclusive(5, 5 + (int) (entropyData.getConcentration(pos) / 10));
                int x = pos.getMinBlockX();
                int z = pos.getMinBlockZ();
                for (int i = 0; i < count; i++) {
                    BlockPos selectedPos = new BlockPos(
                            x + level.random.nextInt(16),
                            level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + level.random.nextIntBetweenInclusive(-5, 0),
                            z + level.random.nextInt(16)
                    );
                    Block selected = level.getBlockState(selectedPos).getBlock();
                    HashChain.Node<Block> chainNode = AGING_CHAINS.find(selected);
                    if (chainNode != null)
                        level.setBlock(selectedPos, (chainNode.next == null ? Blocks.AIR : chainNode.next.value).defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    @SubscribeEvent
    public static void ServerEntropyEntityEffect(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.tickCount % 40 == 0) {
            if ((entity instanceof LivingEntity living) && !entity.level().isClientSide) {
                float concentration = EntropyWorldData.getConcentration((ServerLevel) entity.level(), living.chunkPosition());
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
