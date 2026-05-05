package com.lastchance.downed.network.payload;

import com.lastchance.downed.DownedPlayersMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SurrenderPayload() implements CustomPayload {
    public static final CustomPayload.Id<SurrenderPayload> ID =
            new CustomPayload.Id<>(DownedPlayersMod.id("surrender"));
    public static final PacketCodec<RegistryByteBuf, SurrenderPayload> CODEC =
            PacketCodec.ofStatic(SurrenderPayload::write, SurrenderPayload::read);

    private static SurrenderPayload read(RegistryByteBuf buf) {
        return new SurrenderPayload();
    }

    private static void write(RegistryByteBuf buf, SurrenderPayload payload) {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
