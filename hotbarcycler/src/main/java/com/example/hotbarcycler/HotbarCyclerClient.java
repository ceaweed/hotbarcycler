package com.example.hotbarcycler;

import com.example.hotbarcycler.screen.HotbarCyclerConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class HotbarCyclerClient implements ClientModInitializer {

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("hotbarcycler", "hotbarcycler"));

    // ── Keybindings ───────────────────────────────────────────────────────────
    private static KeyBinding nextHotbarKey;
    private static KeyBinding prevHotbarKey;
    private static KeyBinding nextColumnKey;
    private static KeyBinding prevColumnKey;
    private static KeyBinding openSettingsKey;
    private static KeyBinding toggleColumnPreviewKey;
    private static KeyBinding toggleRowIndicatorKey;
    private static KeyBinding toggleInventoryOverlayKey;

    // ── Runtime state ─────────────────────────────────────────────────────────
    /** Which of the 4 inventory rows is currently active in the hotbar. */
    private static int currentPage = 0;

    /**
     * Runtime visibility for Column Preview.
     * Toggled by toggleColumnPreviewKey; initialised from config on join.
     */
    private static boolean columnPreviewVisible = true;

    /**
     * Runtime visibility for Row Indicator.
     * Toggled by toggleRowIndicatorKey; initialised from config on join.
     */
    private static boolean rowIndicatorVisible = true;

    /**
     * Whether the Inventory Overlay is currently shown.
     * Toggled by toggleInventoryOverlayKey; always starts hidden.
     */
    private static boolean inventoryOverlayVisible = false;

    // ── Getters ───────────────────────────────────────────────────────────────
    public static int     getCurrentPage()             { return currentPage; }
    public static boolean isColumnPreviewVisible()     { return columnPreviewVisible; }
    public static boolean isRowIndicatorVisible()      { return rowIndicatorVisible; }
    public static boolean isInventoryOverlayVisible()  { return inventoryOverlayVisible; }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    public void onInitializeClient() {
        HotbarCyclerConfig.get();   // create config file on first launch

        nextHotbarKey = reg("key.hotbarcycler.next");
        prevHotbarKey = reg("key.hotbarcycler.prev");
        nextColumnKey = reg("key.hotbarcycler.column_next");
        prevColumnKey = reg("key.hotbarcycler.column_prev");
        openSettingsKey          = reg("key.hotbarcycler.open_settings");
        toggleColumnPreviewKey   = reg("key.hotbarcycler.toggle_column_preview");
        toggleRowIndicatorKey    = reg("key.hotbarcycler.toggle_row_indicator");
        toggleInventoryOverlayKey = reg("key.hotbarcycler.toggle_inventory_overlay");

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            currentPage            = 0;
            inventoryOverlayVisible = false;
            // Sync runtime visibility from saved config on world join
            columnPreviewVisible = HotbarCyclerConfig.get().showColumnPreview;
            rowIndicatorVisible  = HotbarCyclerConfig.get().showRowIndicator;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (openSettingsKey.wasPressed()) {
                client.setScreen(new HotbarCyclerConfigScreen(client.currentScreen));
            }
            while (toggleColumnPreviewKey.wasPressed()) {
                columnPreviewVisible = !columnPreviewVisible;
            }
            while (toggleRowIndicatorKey.wasPressed()) {
                rowIndicatorVisible = !rowIndicatorVisible;
            }
            while (toggleInventoryOverlayKey.wasPressed()) {
                inventoryOverlayVisible = !inventoryOverlayVisible;
            }

            while (nextHotbarKey.wasPressed()) {
                ClientPlayNetworking.send(new CycleHotbarPayload(true));
                currentPage = (currentPage + 1) % 4;
            }
            while (prevHotbarKey.wasPressed()) {
                ClientPlayNetworking.send(new CycleHotbarPayload(false));
                currentPage = (currentPage - 1 + 4) % 4;
            }

            int sel = client.player.getInventory().getSelectedSlot();
            while (nextColumnKey.wasPressed()) {
                ClientPlayNetworking.send(new CycleColumnPayload(sel, true));
            }
            while (prevColumnKey.wasPressed()) {
                ClientPlayNetworking.send(new CycleColumnPayload(sel, false));
            }
        });
    }

    private static KeyBinding reg(String id) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                id, InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
    }
}
