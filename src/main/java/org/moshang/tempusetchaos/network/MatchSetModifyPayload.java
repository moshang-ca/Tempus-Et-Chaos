package org.moshang.tempusetchaos.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.api.IMatchable;
import org.moshang.tempusetchaos.util.MatchSet;

public record MatchSetModifyPayload(BlockPos pos, int modifier, MatchSet.Entry entry) implements CustomPacketPayload {
    public static final Type<MatchSetModifyPayload> TYPE  =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "matchset_modify_payload"));

    public static final StreamCodec<FriendlyByteBuf, MatchSetModifyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MatchSetModifyPayload::pos,
                    ByteBufCodecs.INT, MatchSetModifyPayload::modifier,
                    MatchSet.Entry.STREAM_CODEC, MatchSetModifyPayload::entry,
                    MatchSetModifyPayload::new
            );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MatchSetModifyPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player.level().getBlockEntity(payload.pos) instanceof IMatchable matchable)) return;
            if (player.distanceToSqr(payload.pos.getX() + .5,
                    payload.pos.getY(), payload.pos.getZ() + .5) >= 64 * 64) return;

            matchable.modifyMatchSet(payload.entry, payload.modifier);
        });
    }
}
