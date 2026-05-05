package com.lastchance.downed.network.payload;

import com.lastchance.downed.DownedPlayersMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ReviveProgressPayload(boolean active, float progress) implements CustomPayload {
    public static final CustomPayload.Id<ReviveProgressPayload> ID =
            new CustomPayload.Id<>(DownedPlayersMod.id("revive_progress"));
    public static final PacketCodec<RegistryByteBuf, ReviveProgressPayload> CODEC =
            PacketCodec.ofStatic(ReviveProgressPayload::write, ReviveProgressPayload::read);

    private static ReviveProgressPayload read(RegistryByteBuf buf) {
        return new ReviveProgressPayload(buf.readBoolean(), buf.readFloat());
    }

    private static void write(RegistryByteBuf buf, ReviveProgressPayload payload) {
        buf.writeBoolean(payload.active());
        buf.writeFloat(payload.progress());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
