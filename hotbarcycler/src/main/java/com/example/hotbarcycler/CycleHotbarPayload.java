package com.example.hotbarcycler;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server packet that carries a single direction:
 *   true  = Next  (rotate rows upward   → row 9-17 becomes the new hotbar)
 *   false = Prev  (rotate rows downward → row 27-35 becomes the new hotbar)
 */
public record CycleHotbarPayload(boolean next) implements CustomPayload {

    public static final CustomPayload.Id<CycleHotbarPayload> ID =
            new CustomPayload.Id<>(Identifier.of("hotbarcycler", "cycle"));

    public static final PacketCodec<PacketByteBuf, CycleHotbarPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeBoolean(value.next()),
                    buf -> new CycleHotbarPayload(buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
