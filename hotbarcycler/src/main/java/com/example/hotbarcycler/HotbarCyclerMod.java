package com.example.hotbarcycler;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HotbarCyclerMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("hotbarcycler");

    @Override
    public void onInitialize() {
        // --- Hotbar row cycling ---
        PayloadTypeRegistry.playC2S().register(CycleHotbarPayload.ID, CycleHotbarPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CycleHotbarPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (payload.next()) rotateNext(context.player());
                    else                rotatePrev(context.player());
                }));

        // --- Single column cycling ---
        PayloadTypeRegistry.playC2S().register(CycleColumnPayload.ID, CycleColumnPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CycleColumnPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    int slot = payload.selectedSlot();
                    if (slot < 0 || slot > 8) {
                        LOGGER.warn("Invalid column slot {} from {}",
                                slot, context.player().getName().getString());
                        return;
                    }
                    if (payload.next()) rotateColumnNext(context.player(), slot);
                    else                rotateColumnPrev(context.player(), slot);
                }));

        LOGGER.info("Hotbar Cycler initialised.");
    }

    // =========================================================================
    // Full hotbar rotation
    // =========================================================================

    /**
     * Rotates all 36 main slots UPWARD by one row.
     *   Old hotbar (0-8)   → row 3 (27-35)  [wraps to top]
     *   Old row 1  (9-17)  → hotbar (0-8)   [new hotbar]
     *   Old row 2  (18-26) → row 1  (9-17)
     *   Old row 3  (27-35) → row 2  (18-26)
     */
    private static void rotateNext(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> hotbar = copySlots(inv, 0, 9);
        copyIntoSlots(inv, 0,  inv, 9,  9);
        copyIntoSlots(inv, 9,  inv, 18, 9);
        copyIntoSlots(inv, 18, inv, 27, 9);
        setSlots(inv, 27, hotbar);
        sync(player);
    }

    /**
     * Rotates all 36 main slots DOWNWARD by one row.
     *   Old hotbar (0-8)   → row 1  (9-17)
     *   Old row 3  (27-35) → hotbar (0-8)   [new hotbar]
     *   Old row 2  (18-26) → row 3  (27-35)
     *   Old row 1  (9-17)  → row 2  (18-26)
     */
    private static void rotatePrev(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> hotbar = copySlots(inv, 0, 9);
        copyIntoSlots(inv, 0,  inv, 27, 9);
        copyIntoSlots(inv, 27, inv, 18, 9);
        copyIntoSlots(inv, 18, inv, 9,  9);
        setSlots(inv, 9, hotbar);
        sync(player);
    }

    // =========================================================================
    // Single column rotation
    // =========================================================================

    /**
     * Rotates the column for hotbar slot s UPWARD.
     *   slot s      → slot s+27  (hotbar item wraps to row 3)
     *   slot s+9    → slot s     (row 1 becomes new hotbar item)
     *   slot s+18   → slot s+9
     *   slot s+27   → slot s+18
     */
    private static void rotateColumnNext(ServerPlayerEntity player, int s) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> stacks = inv.getMainStacks();
        ItemStack hotbarItem = stacks.get(s).copy();
        stacks.set(s,      stacks.get(s +  9).copy());
        stacks.set(s +  9, stacks.get(s + 18).copy());
        stacks.set(s + 18, stacks.get(s + 27).copy());
        stacks.set(s + 27, hotbarItem);
        sync(player);
    }

    /**
     * Rotates the column for hotbar slot s DOWNWARD.
     *   slot s      → slot s+9   (hotbar item moves to row 1)
     *   slot s+27   → slot s     (row 3 becomes new hotbar item)
     *   slot s+18   → slot s+27
     *   slot s+9    → slot s+18
     */
    private static void rotateColumnPrev(ServerPlayerEntity player, int s) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> stacks = inv.getMainStacks();
        ItemStack hotbarItem = stacks.get(s).copy();
        stacks.set(s,      stacks.get(s + 27).copy());
        stacks.set(s + 27, stacks.get(s + 18).copy());
        stacks.set(s + 18, stacks.get(s +  9).copy());
        stacks.set(s +  9, hotbarItem);
        sync(player);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static List<ItemStack> copySlots(PlayerInventory inv, int start, int count) {
        List<ItemStack> stacks = inv.getMainStacks();
        List<ItemStack> copy = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            copy.add(stacks.get(start + i).copy());
        }
        return copy;
    }

    private static void copyIntoSlots(PlayerInventory dst, int dstStart,
                                      PlayerInventory src, int srcStart, int count) {
        List<ItemStack> dstStacks = dst.getMainStacks();
        List<ItemStack> srcStacks = src.getMainStacks();
        for (int i = 0; i < count; i++) {
            dstStacks.set(dstStart + i, srcStacks.get(srcStart + i).copy());
        }
    }

    private static void setSlots(PlayerInventory inv, int start, List<ItemStack> items) {
        List<ItemStack> stacks = inv.getMainStacks();
        for (int i = 0; i < items.size(); i++) {
            stacks.set(start + i, items.get(i));
        }
    }

    private static void sync(ServerPlayerEntity player) {
        player.getInventory().markDirty();
        player.playerScreenHandler.syncState();
    }
}
