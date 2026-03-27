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
        PayloadTypeRegistry.playC2S().register(CycleHotbarPayload.ID, CycleHotbarPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CycleHotbarPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (payload.next()) rotateNext(context.player());
                    else                rotatePrev(context.player());
                }));

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

    // ── Full hotbar rotations ─────────────────────────────────────────────────
    // Using getStack/setStack instead of getMainStacks() for 1.21.1 compatibility

    private static void rotateNext(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> hotbar = copySlots(inv, 0, 9);
        moveSlots(inv, 0,  inv, 9,  9);
        moveSlots(inv, 9,  inv, 18, 9);
        moveSlots(inv, 18, inv, 27, 9);
        setSlots(inv, 27, hotbar);
        sync(player);
    }

    private static void rotatePrev(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> hotbar = copySlots(inv, 0, 9);
        moveSlots(inv, 0,  inv, 27, 9);
        moveSlots(inv, 27, inv, 18, 9);
        moveSlots(inv, 18, inv, 9,  9);
        setSlots(inv, 9, hotbar);
        sync(player);
    }

    // ── Column rotations ──────────────────────────────────────────────────────

    private static void rotateColumnNext(ServerPlayerEntity player, int s) {
        PlayerInventory inv = player.getInventory();
        ItemStack hotbarItem = inv.getStack(s).copy();
        inv.setStack(s,      inv.getStack(s +  9).copy());
        inv.setStack(s +  9, inv.getStack(s + 18).copy());
        inv.setStack(s + 18, inv.getStack(s + 27).copy());
        inv.setStack(s + 27, hotbarItem);
        sync(player);
    }

    private static void rotateColumnPrev(ServerPlayerEntity player, int s) {
        PlayerInventory inv = player.getInventory();
        ItemStack hotbarItem = inv.getStack(s).copy();
        inv.setStack(s,      inv.getStack(s + 27).copy());
        inv.setStack(s + 27, inv.getStack(s + 18).copy());
        inv.setStack(s + 18, inv.getStack(s +  9).copy());
        inv.setStack(s +  9, hotbarItem);
        sync(player);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<ItemStack> copySlots(PlayerInventory inv, int start, int count) {
        List<ItemStack> copy = new ArrayList<>(count);
        for (int i = 0; i < count; i++) copy.add(inv.getStack(start + i).copy());
        return copy;
    }

    private static void moveSlots(PlayerInventory dst, int dstStart,
                                  PlayerInventory src, int srcStart, int count) {
        for (int i = 0; i < count; i++)
            dst.setStack(dstStart + i, src.getStack(srcStart + i).copy());
    }

    private static void setSlots(PlayerInventory inv, int start, List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) inv.setStack(start + i, items.get(i));
    }

    private static void sync(ServerPlayerEntity player) {
        player.getInventory().markDirty();
        player.playerScreenHandler.syncState();
    }
}
