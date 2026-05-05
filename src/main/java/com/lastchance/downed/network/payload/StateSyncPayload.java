package com.lastchance.downed.network.payload;

import com.lastchance.downed.DownedPlayersMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record StateSyncPayload(boolean downed, long enteredAtMillis, long deadlineMillis, boolean surrenderEnabled)
        implements CustomPayload {
    public static final CustomPayload.Id<StateSyncPayload> ID =
            new CustomPayload.Id<>(DownedPlayersMod.id("state_sync"));
    public static final PacketCodec<RegistryByteBuf, StateSyncPayload> CODEC =
            PacketCodec.ofStatic(StateSyncPayload::write, StateSyncPayload::read);

    private static StateSyncPayload read(RegistryByteBuf buf) {
        return new StateSyncPayload(buf.readBoolean(), buf.readVarLong(), buf.readVarLong(), buf.readBoolean());
    }

    private static void write(RegistryByteBuf buf, StateSyncPayload payload) {
        buf.writeBoolean(payload.downed());
        buf.writeVarLong(payload.enteredAtMillis());
        buf.writeVarLong(payload.deadlineMillis());
        buf.writeBoolean(payload.surrenderEnabled());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
