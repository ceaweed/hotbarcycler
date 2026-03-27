package com.example.hotbarcycler;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HotbarCyclerMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("hotbarcycler");

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(CycleHotbarPayload.ID,
                (server, player, handler, buf, responseSender) -> {
                    boolean next = buf.readBoolean();
                    server.execute(() -> {
                        if (next) rotateNext(player);
                        else      rotatePrev(player);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(CycleColumnPayload.ID,
                (server, player, handler, buf, responseSender) -> {
                    int     slot = buf.readInt();
                    boolean next = buf.readBoolean();
                    server.execute(() -> {
                        if (slot < 0 || slot > 8) {
                            LOGGER.warn("Invalid column slot {} from {}",
                                    slot, player.getName().getString());
                            return;
                        }
                        if (next) rotateColumnNext(player, slot);
                        else      rotateColumnPrev(player, slot);
                    });
                });

        LOGGER.info("Hotbar Cycler initialised.");
    }

    // ── Full hotbar rotations ─────────────────────────────────────────────────
    // Operate directly on main[] to avoid setStack() firing markDirty() 36 times,
    // which can cause syncState() to see a fully-tracked (no-op) diff at the end.

    private static void rotateNext(ServerPlayerEntity player) {
        DefaultedList<ItemStack> main = player.getInventory().main;
        // Save hotbar (slots 0-8)
        ItemStack[] saved = snapshot(main, 0, 9);
        // Shift each row toward the hotbar
        copy(main, 0,  main, 9,  9);   // hotbar  ← row1
        copy(main, 9,  main, 18, 9);   // row1    ← row2
        copy(main, 18, main, 27, 9);   // row2    ← row3
        paste(main, 27, saved);         // row3    ← old hotbar
        sync(player);
    }

    private static void rotatePrev(ServerPlayerEntity player) {
        DefaultedList<ItemStack> main = player.getInventory().main;
        // Save hotbar (slots 0-8)
        ItemStack[] saved = snapshot(main, 0, 9);
        // Shift each row away from the hotbar
        copy(main, 0,  main, 27, 9);   // hotbar  ← row3
        copy(main, 27, main, 18, 9);   // row3    ← row2  (reads untouched 18-26 ✓)
        copy(main, 18, main, 9,  9);   // row2    ← row1  (reads untouched 9-17  ✓)
        paste(main, 9, saved);          // row1    ← old hotbar
        sync(player);
    }

    // ── Column rotations ──────────────────────────────────────────────────────
    // Save the hotbar slot first, then do all reads before writes to avoid
    // the cascading read-after-write hazard that plagued the old setStack version.

    private static void rotateColumnNext(ServerPlayerEntity player, int s) {
        DefaultedList<ItemStack> main = player.getInventory().main;
        // Snapshot all four slots up front
        ItemStack h  = main.get(s     ).copy();
        ItemStack r1 = main.get(s +  9).copy();
        ItemStack r2 = main.get(s + 18).copy();
        ItemStack r3 = main.get(s + 27).copy();
        // Rotate: hotbar←r1, r1←r2, r2←r3, r3←hotbar
        main.set(s,      r1);
        main.set(s +  9, r2);
        main.set(s + 18, r3);
        main.set(s + 27, h);
        sync(player);
    }

    private static void rotateColumnPrev(ServerPlayerEntity player, int s) {
        DefaultedList<ItemStack> main = player.getInventory().main;
        // Snapshot all four slots up front
        ItemStack h  = main.get(s     ).copy();
        ItemStack r1 = main.get(s +  9).copy();
        ItemStack r2 = main.get(s + 18).copy();
        ItemStack r3 = main.get(s + 27).copy();
        // Rotate: hotbar←r3, r3←r2, r2←r1, r1←hotbar
        main.set(s,      r3);
        main.set(s +  9, h);
        main.set(s + 18, r1);
        main.set(s + 27, r2);
        sync(player);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns copies of [start, start+count). */
    private static ItemStack[] snapshot(DefaultedList<ItemStack> list, int start, int count) {
        ItemStack[] out = new ItemStack[count];
        for (int i = 0; i < count; i++) out[i] = list.get(start + i).copy();
        return out;
    }

    /**
     * Copies [srcStart, srcStart+count) → [dstStart, …).
     * Safe when dst==src as long as the ranges don't overlap.
     */
    private static void copy(DefaultedList<ItemStack> dst, int dstStart,
                             DefaultedList<ItemStack> src, int srcStart, int count) {
        for (int i = 0; i < count; i++)
            dst.set(dstStart + i, src.get(srcStart + i).copy());
    }

    /** Writes a pre-copied snapshot array back into [start, …). */
    private static void paste(DefaultedList<ItemStack> list, int start, ItemStack[] items) {
        for (int i = 0; i < items.length; i++) list.set(start + i, items[i]);
    }

    /**
     * Marks dirty once (all slot writes are already done) then syncs the
     * full player screen handler to the client in one go.
     */
    private static void sync(ServerPlayerEntity player) {
        player.getInventory().markDirty();
        player.playerScreenHandler.syncState();
    }
}