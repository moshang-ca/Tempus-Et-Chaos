package org.moshang.tempusetchaos.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.TempusEtChaos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record ChrononNetworkSyncPayload(UUID uuid, long stored, long capacity) implements CustomPacketPayload {
    public static final Type<ChrononNetworkSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "chronon_network_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChrononNetworkSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, ChrononNetworkSyncPayload::uuid,
                    ByteBufCodecs.VAR_LONG, ChrononNetworkSyncPayload::stored,
                    ByteBufCodecs.VAR_LONG, ChrononNetworkSyncPayload::capacity,
                    ChrononNetworkSyncPayload::new
            );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ClientCache(long stored, long capacity) {
        private static final Map<UUID, ClientCache> CACHE = new ConcurrentHashMap<>();
        private static final ClientCache DEFAULT = new ClientCache(0L, 0L);

        public static void handle(ChrononNetworkSyncPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> CACHE.put(payload.uuid(), new ClientCache(payload.stored, payload.capacity)));
        }

        public static ClientCache get(UUID uuid) {
            return CACHE.getOrDefault(uuid, DEFAULT);
        }

        public static void clear() {
            CACHE.clear();
        }
    }
}
