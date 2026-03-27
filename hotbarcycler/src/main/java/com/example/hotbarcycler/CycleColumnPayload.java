package com.example.hotbarcycler;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** Replaced CustomPayload — just a channel ID + encoder helper. */
public final class CycleColumnPayload {

    public static final Identifier ID = new Identifier("hotbarcycler", "cycle_column");

    private CycleColumnPayload() {}

    public static PacketByteBuf encode(int selectedSlot, boolean next) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(selectedSlot);
        buf.writeBoolean(next);
        return buf;
    }
}
