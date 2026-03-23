package com.example.hotbarcycler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent config for Hotbar Cycler. Saved to config/hotbarcycler.json.
 * All offsets are GUI (scaled) pixels.  +X = right,  +Y = down.
 */
public class HotbarCyclerConfig {

    // ── Column Preview ────────────────────────────────────────────────────────
    public boolean showColumnPreview       = true;
    public boolean showEmptyColumns        = false;
    public int     columnPreviewOffsetX    = 0;
    public int     columnPreviewOffsetY    = 0;
    public int     columnPreviewSlotWidth  = 20;
    public int     columnPreviewSlotHeight = 20;
    /** Background opacity for column preview slots (0 = invisible, 255 = opaque). */
    /** 0 = transparent style. 255 = solid dark fill. */
    public int     columnPreviewAlpha = 0;

    // ── Row Indicator ─────────────────────────────────────────────────────────
    public boolean showRowIndicator        = true;
    public int     rowIndicatorOffsetX     = 0;
    public int     rowIndicatorOffsetY     = 0;
    public int     rowIndicatorSquareW     = 5;
    public int     rowIndicatorSquareH     = 5;

    // ── Inventory Overlay ─────────────────────────────────────────────────────
    /** X offset from screen centre for the overlay panel. */
    public int     inventoryOverlayOffsetX = 0;
    /** Y offset from screen centre for the overlay panel. */
    public int     inventoryOverlayOffsetY = 0;
    /** Pixel size of each inventory slot. */
    public int     inventoryOverlaySlotSize = 20;
    /** Number of slots per row (1–9). Vanilla inventory uses 9. */
    public int     inventoryOverlaySlotsPerRow = 9;
    /** Extra pixels of gap between the 3 inventory rows and the hotbar row at the bottom. */
    public int     inventoryOverlayHotbarPadding = 6;
    /** Background opacity for the inventory overlay panel (0 = invisible, 255 = opaque). */
    /** 0 = transparent-inventory style (world shows through). 255 = solid dark panel. */
    /** 0 = transparent style. 255 = solid dark fill. */
    public int     inventoryOverlayAlpha = 0;

    // ── Persistence ──────────────────────────────────────────────────────────
    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hotbarcycler.json";
    private static HotbarCyclerConfig instance;

    public static HotbarCyclerConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static HotbarCyclerConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                HotbarCyclerConfig cfg = GSON.fromJson(r, HotbarCyclerConfig.class);
                if (cfg != null) return cfg;
            } catch (IOException | com.google.gson.JsonParseException e) {
                HotbarCyclerMod.LOGGER.warn("Failed to load config, using defaults: {}", e.getMessage());
            }
        }
        HotbarCyclerConfig cfg = new HotbarCyclerConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            HotbarCyclerMod.LOGGER.warn("Failed to save config: {}", e.getMessage());
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
