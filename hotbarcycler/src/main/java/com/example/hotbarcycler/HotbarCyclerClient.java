package com.example.hotbarcycler;

import com.example.hotbarcycler.screen.HotbarCyclerConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

public class HotbarCyclerClient implements ClientModInitializer {

    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("key.categories.hotbarcycler"));

    /**
     * PlayerScreenHandler screen-slot offsets for each inventory section.
     *
     * The player screen handler lays out slots as:
     *   0       = crafting result
     *   1-4     = crafting grid
     *   5-8     = armor
     *   9-35    = main inventory rows 1-3  (PlayerInventory.main[9..35])
     *   36-44   = hotbar                  (PlayerInventory.main[0..8])
     *   45      = offhand
     *
     * SlotActionType.SWAP with button 0-8 swaps slotId ↔ hotbar[button].
     * Three such swaps chain-cycle the four rows with zero stacking risk.
     */
    private static final int HOTBAR = 36;  // unused directly but kept for clarity
    private static final int ROW1   = 9;
    private static final int ROW2   = 18;
    private static final int ROW3   = 27;

    private static KeyBinding nextHotbarKey;
    private static KeyBinding prevHotbarKey;
    private static KeyBinding nextColumnKey;
    private static KeyBinding prevColumnKey;
    private static KeyBinding openSettingsKey;
    private static KeyBinding toggleColumnPreviewKey;
    private static KeyBinding toggleRowIndicatorKey;
    private static KeyBinding toggleInventoryOverlayKey;

    private static int     currentPage             = 0;
    private static boolean columnPreviewVisible    = true;
    private static boolean rowIndicatorVisible     = true;
    private static boolean inventoryOverlayVisible = false;

    public static int     getCurrentPage()            { return currentPage; }
    public static boolean isColumnPreviewVisible()    { return columnPreviewVisible; }
    public static boolean isRowIndicatorVisible()     { return rowIndicatorVisible; }
    public static boolean isInventoryOverlayVisible() { return inventoryOverlayVisible; }

    @Override
    public void onInitializeClient() {
        HotbarCyclerConfig.get();

        nextHotbarKey             = reg("key.hotbarcycler.next");
        prevHotbarKey             = reg("key.hotbarcycler.prev");
        nextColumnKey             = reg("key.hotbarcycler.column_next");
        prevColumnKey             = reg("key.hotbarcycler.column_prev");
        openSettingsKey           = reg("key.hotbarcycler.open_settings");
        toggleColumnPreviewKey    = reg("key.hotbarcycler.toggle_column_preview");
        toggleRowIndicatorKey     = reg("key.hotbarcycler.toggle_row_indicator");
        toggleInventoryOverlayKey = reg("key.hotbarcycler.toggle_inventory_overlay");

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            currentPage             = 0;
            inventoryOverlayVisible = false;
            columnPreviewVisible    = HotbarCyclerConfig.get().showColumnPreview;
            rowIndicatorVisible     = HotbarCyclerConfig.get().showRowIndicator;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (openSettingsKey.wasPressed())
                client.setScreen(new HotbarCyclerConfigScreen(client.currentScreen));
            while (toggleColumnPreviewKey.wasPressed())
                columnPreviewVisible = !columnPreviewVisible;
            while (toggleRowIndicatorKey.wasPressed())
                rowIndicatorVisible = !rowIndicatorVisible;
            while (toggleInventoryOverlayKey.wasPressed())
                inventoryOverlayVisible = !inventoryOverlayVisible;

            while (nextHotbarKey.wasPressed()) {
                rotateRows(client, true);
                currentPage = (currentPage + 1) % 4;
            }
            while (prevHotbarKey.wasPressed()) {
                rotateRows(client, false);
                currentPage = (currentPage - 1 + 4) % 4;
            }

            int sel = client.player.getInventory().getSelectedSlot();
            while (nextColumnKey.wasPressed())
                rotateColumn(client, sel, true);
            while (prevColumnKey.wasPressed())
                rotateColumn(client, sel, false);
        });
    }

    /**
     * Rotates all 9 inventory columns by one row.
     *
     * Uses SlotActionType.SWAP: clickSlot(syncId, slotId, button=col, SWAP, player)
     * swaps the item at screen slot [slotId] with hotbar slot [col].
     *
     * next=true  (↑): hotbar ← row1 ← row2 ← row3 ← old hotbar
     *   swap(row3[c], c) → hotbar[c]=row3, row3[c]=hotbar
     *   swap(row2[c], c) → hotbar[c]=row2, row2[c]=old row3 ✓
     *   swap(row1[c], c) → hotbar[c]=row1 ✓, row1[c]=old row2 ✓
     *   result: hotbar=row1, row1=row2, row2=row3, row3=old hotbar ✓
     *
     * next=false (↓): hotbar ← row3 ← row2 ← row1 ← old hotbar
     *   swap(row1[c], c) → hotbar[c]=row1, row1[c]=hotbar
     *   swap(row2[c], c) → hotbar[c]=row2, row2[c]=old row1 ✓
     *   swap(row3[c], c) → hotbar[c]=row3 ✓, row3[c]=old row2 ✓
     *   result: hotbar=row3, row3=row2, row2=row1, row1=old hotbar ✓
     */
    private static void rotateRows(MinecraftClient client, boolean next) {
        int syncId = client.player.playerScreenHandler.syncId;
        for (int c = 0; c < 9; c++) {
            if (next) {
                swap(client, syncId, ROW3 + c, c);
                swap(client, syncId, ROW2 + c, c);
                swap(client, syncId, ROW1 + c, c);
            } else {
                swap(client, syncId, ROW1 + c, c);
                swap(client, syncId, ROW2 + c, c);
                swap(client, syncId, ROW3 + c, c);
            }
        }
    }

    /**
     * Rotates a single inventory column (same SWAP chain, one column only).
     */
    private static void rotateColumn(MinecraftClient client, int col, boolean next) {
        int syncId = client.player.playerScreenHandler.syncId;
        if (next) {
            swap(client, syncId, ROW3 + col, col);
            swap(client, syncId, ROW2 + col, col);
            swap(client, syncId, ROW1 + col, col);
        } else {
            swap(client, syncId, ROW1 + col, col);
            swap(client, syncId, ROW2 + col, col);
            swap(client, syncId, ROW3 + col, col);
        }
    }

    /**
     * Fires a SWAP click: swaps screen slot [slotId] with hotbar slot [hotbarIndex].
     * clickSlot() updates the client inventory immediately and sends a
     * ClickSlotC2SPacket so the server stays in sync — no custom networking needed.
     */
    private static void swap(MinecraftClient client, int syncId, int slotId, int hotbarIndex) {
        client.interactionManager.clickSlot(
                syncId, slotId, hotbarIndex, SlotActionType.SWAP, client.player);
    }

    private static KeyBinding reg(String id) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                id, InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
    }
}