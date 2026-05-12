package com.lastchance.downed.network.payload;

import com.lastchance.downed.DownedPlayersMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record HandsUpTogglePayload() implements CustomPayload {
    public static final CustomPayload.Id<HandsUpTogglePayload> ID =
            new CustomPayload.Id<>(DownedPlayersMod.id("hands_up_toggle"));
    public static final PacketCodec<RegistryByteBuf, HandsUpTogglePayload> CODEC =
            PacketCodec.ofStatic(HandsUpTogglePayload::write, HandsUpTogglePayload::read);

    private static HandsUpTogglePayload read(RegistryByteBuf buf) {
        return new HandsUpTogglePayload();
    }

    private static void write(RegistryByteBuf buf, HandsUpTogglePayload payload) {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
