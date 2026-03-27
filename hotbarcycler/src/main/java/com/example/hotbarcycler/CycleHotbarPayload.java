package com.example.hotbarcycler;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** Replaced CustomPayload — just a channel ID + encoder helper. */
public final class CycleHotbarPayload {

    public static final Identifier ID = new Identifier("hotbarcycler", "cycle");

    private CycleHotbarPayload() {}

    public static PacketByteBuf encode(boolean next) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(next);
        return buf;
    }
}
