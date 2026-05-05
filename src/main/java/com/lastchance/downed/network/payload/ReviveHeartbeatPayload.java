package com.lastchance.downed.network.payload;

import com.lastchance.downed.DownedPlayersMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record ReviveHeartbeatPayload(UUID targetUuid) implements CustomPayload {
    public static final CustomPayload.Id<ReviveHeartbeatPayload> ID =
            new CustomPayload.Id<>(DownedPlayersMod.id("revive_heartbeat"));
    public static final PacketCodec<RegistryByteBuf, ReviveHeartbeatPayload> CODEC =
            PacketCodec.ofStatic(ReviveHeartbeatPayload::write, ReviveHeartbeatPayload::read);

    private static ReviveHeartbeatPayload read(RegistryByteBuf buf) {
        return new ReviveHeartbeatPayload(buf.readUuid());
    }

    private static void write(RegistryByteBuf buf, ReviveHeartbeatPayload payload) {
        buf.writeUuid(payload.targetUuid());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
