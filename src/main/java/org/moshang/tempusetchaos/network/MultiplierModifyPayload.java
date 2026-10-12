package org.moshang.tempusetchaos.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.blockentity.BEAccelerator;
import org.moshang.tempusetchaos.blockentity.BEReducer;

public record MultiplierModifyPayload(BlockPos pos, int multiplier) implements CustomPacketPayload {
    public static final Type<MultiplierModifyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "multiplier_payload"));

    public static final StreamCodec<FriendlyByteBuf, MultiplierModifyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MultiplierModifyPayload::pos,
                    ByteBufCodecs.INT, MultiplierModifyPayload::multiplier,
                    MultiplierModifyPayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MultiplierModifyPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            BlockEntity be = player.level().getBlockEntity(payload.pos);
            if (player.distanceToSqr(payload.pos.getX() + .5,
                    payload.pos.getY(), payload.pos.getZ() + .5) >= 64 * 64) return;
            if (be instanceof BEAccelerator bea) bea.setAccelerateMultiplier(payload.multiplier);
            else if (be instanceof BEReducer ber) ber.setReduceMultiplier(payload.multiplier);
        });
    }
}
