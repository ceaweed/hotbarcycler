package com.example.hotbarcycler;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server packet for rotating a single inventory column.
 *
 * The four slots in the column for hotbar slot index s are:
 *   s       (hotbar,  main[s])
 *   s +  9  (row 1,   main[s+9])
 *   s + 18  (row 2,   main[s+18])
 *   s + 27  (row 3,   main[s+27])
 *
 *   next=true  → rotate upward:   hotbar→row3, row1→hotbar, row2→row1, row3→row2
 *   next=false → rotate downward: hotbar→row1, row3→hotbar, row2→row3, row1→row2
 */
public record CycleColumnPayload(int selectedSlot, boolean next) implements CustomPayload {

    public static final CustomPayload.Id<CycleColumnPayload> ID =
            new CustomPayload.Id<>(Identifier.of("hotbarcycler", "cycle_column"));

    public static final PacketCodec<PacketByteBuf, CycleColumnPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.selectedSlot());
                        buf.writeBoolean(value.next());
                    },
                    buf -> new CycleColumnPayload(buf.readInt(), buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
