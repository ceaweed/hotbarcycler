package com.example.hotbarcycler.screen;

import com.example.hotbarcycler.HotbarCyclerConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabbed settings screen.  Labels and descriptions are drawn inside
 * super.render() via a post-draw hook so they always appear on top of
 * the dark background overlay, fixing the invisible-text bug.
 */
public class HotbarCyclerConfigScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int TAB_W        = 118;
    private static final int TAB_H        = 20;
    private static final int BTN_W        = 150;
    private static final int BTN_H        = 20;
    private static final int FIELD_W      = 48;
    private static final int FIELD_H      = 18;
    /** Vertical space for one label + field + description line */
    private static final int ITEM_H       = 42;
    private static final int CONTENT_TOP  = 50;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_WHITE   = 0xFFFFFF;
    private static final int C_GOLD    = 0xFFD700;
    private static final int C_GRAY    = 0xAAAAAA;
    private static final int C_DIMGRAY = 0x777777;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private int activeTab = 0;  // 0=Column  1=Row  2=Inventory

    // ── Widgets per tab ───────────────────────────────────────────────────────
    // Column Preview
    private ButtonWidget    colShowBtn, colEmptyBtn;
    private TextFieldWidget colOffX, colOffY, colSlotW, colSlotH, colAlpha;
    // Row Indicator
    private ButtonWidget    rowShowBtn;
    private TextFieldWidget rowOffX, rowOffY, rowSqW, rowSqH;
    // Inventory Overlay
    private TextFieldWidget invOffX, invOffY, invSlotSize, invSlotsPerRow,
                            invHotbarPad, invAlpha;

    // ── Working booleans ──────────────────────────────────────────────────────
    private boolean wColShow, wColEmpty, wRowShow;

    // ── Post-render text entries (drawn AFTER super.render so they show on top)
    private record LabelEntry(int cx, int y, String text, int color, boolean shadow) {}
    private final List<LabelEntry> postLabels = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    public HotbarCyclerConfigScreen(Screen parent) {
        super(Text.literal("Hotbar Cycler Settings"));
        this.parent = parent;
    }

    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public boolean shouldPause()      { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        postLabels.clear();
        // Null-out all widget references so saveAndClose() guards work correctly
        colShowBtn = null; colEmptyBtn = null;
        colOffX = null; colOffY = null; colSlotW = null; colSlotH = null; colAlpha = null;
        rowShowBtn = null;
        rowOffX = null; rowOffY = null; rowSqW = null; rowSqH = null;
        invOffX = null; invOffY = null; invSlotSize = null; invSlotsPerRow = null;
        invHotbarPad = null; invAlpha = null;

        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
        wColShow  = cfg.showColumnPreview;
        wColEmpty = cfg.showEmptyColumns;
        wRowShow  = cfg.showRowIndicator;

        int cx = width / 2;

        // ── Tab bar ──────────────────────────────────────────────────────────
        String[] tabNames = {"Column Preview", "Row Indicator", "Inventory Overlay"};
        int totalW  = tabNames.length * TAB_W + (tabNames.length - 1) * 2;
        int tabStartX = cx - totalW / 2;
        for (int t = 0; t < tabNames.length; t++) {
            final int tab = t;
            addDrawableChild(ButtonWidget.builder(Text.literal(tabNames[t]), b -> {
                activeTab = tab; clearChildren(); init();
            }).dimensions(tabStartX + t * (TAB_W + 2), 22, TAB_W, TAB_H).build());
        }

        // ── Tab content ──────────────────────────────────────────────────────
        switch (activeTab) {
            case 0 -> buildColumnTab(cfg, cx);
            case 1 -> buildRowTab(cfg, cx);
            case 2 -> buildInventoryTab(cfg, cx);
        }

        // ── Bottom buttons ───────────────────────────────────────────────────
        int by = height - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"),
                b -> saveAndClose()).dimensions(cx - BTN_W - 4, by, BTN_W, BTN_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                b -> close()).dimensions(cx + 4, by, BTN_W, BTN_H).build());
    }

    // =========================================================================
    // Tab builders
    // =========================================================================

    private void buildColumnTab(HotbarCyclerConfig cfg, int cx) {
        int y = CONTENT_TOP;

        // ── Visibility section ───────────────────────────────────────────────
        sectionLabel("-- Visibility --", cx, y); y += 13;

        colShowBtn = addToggle(cx, y, "Show Column Preview", wColShow, b -> {
            wColShow = !wColShow; b.setMessage(toggleMsg("Show Column Preview", wColShow));
        });
        smallLabel("Enables the 3-slot strip below the hotbar showing items above the selected slot.", cx, y + BTN_H + 2);
        y += BTN_H + 14;

        colEmptyBtn = addToggle(cx, y, "Show Empty Slots", wColEmpty, b -> {
            wColEmpty = !wColEmpty; b.setMessage(toggleMsg("Show Empty Slots", wColEmpty));
        });
        smallLabel("When ON: strip stays visible even when all 3 column slots are empty.", cx, y + BTN_H + 2);
        y += BTN_H + 18;

        // ── Position section ─────────────────────────────────────────────────
        sectionLabel("-- Position --", cx, y); y += 13;

        fieldRow("Offset X  (horizontal, pixels)",
                 "Offset Y  (vertical, pixels)",
                 cx, y, cfg.columnPreviewOffsetX, cfg.columnPreviewOffsetY);
        colOffX = fieldLeft; colOffY = fieldRight;
        smallLabel("X shifts left(−)/right(+). Y shifts up(−)/down(+) from screen bottom-centre.", cx, y + FIELD_H + 4);
        y += ITEM_H;

        // ── Size section ─────────────────────────────────────────────────────
        sectionLabel("-- Size --", cx, y); y += 13;

        fieldRow("Slot Width  (pixels, min 16)",
                 "Slot Height  (pixels, min 16)",
                 cx, y, cfg.columnPreviewSlotWidth, cfg.columnPreviewSlotHeight);
        colSlotW = fieldLeft; colSlotH = fieldRight;
        smallLabel("Width and height of each slot box. Items are always 16×16, centred inside.", cx, y + FIELD_H + 4);
        y += ITEM_H;

        // ── Appearance section ───────────────────────────────────────────────
        sectionLabel("-- Appearance --", cx, y); y += 13;

        singleField("Background Opacity  (0 = no background   →   255 = dark opaque)",
                    cx, y, cfg.columnPreviewAlpha);
        colAlpha = fieldSingle;
        smallLabel("0 = background hidden entirely. 255 = solid dark grey. Default: 204.", cx, y + FIELD_H + 4);
    }

    private void buildRowTab(HotbarCyclerConfig cfg, int cx) {
        int y = CONTENT_TOP;

        sectionLabel("-- Visibility --", cx, y); y += 13;

        rowShowBtn = addToggle(cx, y, "Show Row Indicator", wRowShow, b -> {
            wRowShow = !wRowShow; b.setMessage(toggleMsg("Show Row Indicator", wRowShow));
        });
        smallLabel("Toggles the 4 small squares to the left of the hotbar.", cx, y + BTN_H + 2);
        smallLabel("The bright white square shows which inventory row is currently active.", cx, y + BTN_H + 11);
        y += BTN_H + 22;

        sectionLabel("-- Position --", cx, y); y += 13;

        fieldRow("Offset X  (horizontal, pixels)",
                 "Offset Y  (vertical, pixels)",
                 cx, y, cfg.rowIndicatorOffsetX, cfg.rowIndicatorOffsetY);
        rowOffX = fieldLeft; rowOffY = fieldRight;
        smallLabel("X shifts the squares left(−)/right(+). Y shifts up(−)/down(+).", cx, y + FIELD_H + 4);
        y += ITEM_H;

        sectionLabel("-- Size --", cx, y); y += 13;

        fieldRow("Square Width  (pixels, min 1)",
                 "Square Height  (pixels, min 1)",
                 cx, y, cfg.rowIndicatorSquareW, cfg.rowIndicatorSquareH);
        rowSqW = fieldLeft; rowSqH = fieldRight;
        smallLabel("Pixel dimensions of each square. Default: 5×5.", cx, y + FIELD_H + 4);
    }

    private void buildInventoryTab(HotbarCyclerConfig cfg, int cx) {
        int y = CONTENT_TOP;

        smallLabel("Toggle with the 'Toggle Inventory Overlay' keybind (Controls menu).", cx, y);
        smallLabel("Shows all 36 inventory slots without opening the inventory screen.", cx, y + 9);
        y += 22;

        sectionLabel("-- Position --", cx, y); y += 13;

        fieldRow("Offset X  (horizontal, pixels)",
                 "Offset Y  (vertical, pixels)",
                 cx, y, cfg.inventoryOverlayOffsetX, cfg.inventoryOverlayOffsetY);
        invOffX = fieldLeft; invOffY = fieldRight;
        smallLabel("Moves the panel from screen centre. 0,0 = perfectly centred.", cx, y + FIELD_H + 4);
        y += ITEM_H;

        sectionLabel("-- Size --", cx, y); y += 13;

        fieldRow("Slot Size  (pixels, min 16)",
                 "Slots Per Row  (1 – 9)",
                 cx, y, cfg.inventoryOverlaySlotSize, cfg.inventoryOverlaySlotsPerRow);
        invSlotSize = fieldLeft; invSlotsPerRow = fieldRight;
        smallLabel("Slot Size: box size in pixels. Slots Per Row: columns per row (vanilla=9).", cx, y + FIELD_H + 4);
        y += ITEM_H;

        sectionLabel("-- Hotbar Separator --", cx, y); y += 13;

        singleField("Hotbar Padding  (extra pixels between inv rows and hotbar row)",
                    cx, y, cfg.inventoryOverlayHotbarPadding);
        invHotbarPad = fieldSingle;
        smallLabel("Adds space between the 3 inventory rows and the hotbar row at the bottom. Default: 6.", cx, y + FIELD_H + 4);
        y += ITEM_H;

        sectionLabel("-- Appearance --", cx, y); y += 13;

        singleField("Background Opacity  (0 = no background   →   255 = dark opaque)",
                    cx, y, cfg.inventoryOverlayAlpha);
        invAlpha = fieldSingle;
        smallLabel("0 = panel background hidden. 255 = solid dark grey. Default: 221.", cx, y + FIELD_H + 4);
    }

    // =========================================================================
    // Render  — labels drawn AFTER super.render() so they sit on top of widgets
    // =========================================================================

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        // Draw title
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 7, C_WHITE);

        // Draw widgets (buttons, fields)
        super.render(ctx, mouseX, mouseY, delta);

        // Draw our labels LAST so they are never obscured by the background quad
        for (LabelEntry e : postLabels) {
            if (e.shadow()) {
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(e.text()), e.cx(), e.y(), e.color());
            } else {
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(e.text()), e.cx(), e.y(), e.color());
            }
        }
    }

    // =========================================================================
    // Save
    // =========================================================================

    private void saveAndClose() {
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();

        if (colShowBtn  != null) cfg.showColumnPreview       = wColShow;
        if (colEmptyBtn != null) cfg.showEmptyColumns        = wColEmpty;
        if (colOffX     != null) cfg.columnPreviewOffsetX    = parseInt(colOffX,    0);
        if (colOffY     != null) cfg.columnPreviewOffsetY    = parseInt(colOffY,    0);
        if (colSlotW    != null) cfg.columnPreviewSlotWidth  = Math.max(16, parseInt(colSlotW, 20));
        if (colSlotH    != null) cfg.columnPreviewSlotHeight = Math.max(16, parseInt(colSlotH, 20));
        if (colAlpha    != null) cfg.columnPreviewAlpha      = clamp(parseInt(colAlpha, 204), 0, 255);

        if (rowShowBtn  != null) cfg.showRowIndicator        = wRowShow;
        if (rowOffX     != null) cfg.rowIndicatorOffsetX     = parseInt(rowOffX,    0);
        if (rowOffY     != null) cfg.rowIndicatorOffsetY     = parseInt(rowOffY,    0);
        if (rowSqW      != null) cfg.rowIndicatorSquareW     = Math.max(1, parseInt(rowSqW, 5));
        if (rowSqH      != null) cfg.rowIndicatorSquareH     = Math.max(1, parseInt(rowSqH, 5));

        if (invOffX       != null) cfg.inventoryOverlayOffsetX      = parseInt(invOffX,       0);
        if (invOffY       != null) cfg.inventoryOverlayOffsetY      = parseInt(invOffY,       0);
        if (invSlotSize   != null) cfg.inventoryOverlaySlotSize     = Math.max(16, parseInt(invSlotSize, 20));
        if (invSlotsPerRow != null) cfg.inventoryOverlaySlotsPerRow = clamp(parseInt(invSlotsPerRow, 9), 1, 9);
        if (invHotbarPad  != null) cfg.inventoryOverlayHotbarPadding = Math.max(0, parseInt(invHotbarPad, 6));
        if (invAlpha      != null) cfg.inventoryOverlayAlpha        = clamp(parseInt(invAlpha, 221), 0, 255);

        cfg.save();
        close();
    }

    // =========================================================================
    // Widget / label helpers
    // =========================================================================

    /** Gold section header centred on cx. */
    private void sectionLabel(String text, int cx, int y) {
        postLabels.add(new LabelEntry(cx, y, text, C_GOLD, true));
    }

    /** Small grey description line centred on cx. */
    private void smallLabel(String text, int cx, int y) {
        postLabels.add(new LabelEntry(cx, y, text, C_GRAY, false));
    }

    private ButtonWidget addToggle(int cx, int y, String label, boolean state,
                                   ButtonWidget.PressAction action) {
        return addDrawableChild(ButtonWidget.builder(toggleMsg(label, state), action)
                .dimensions(cx - BTN_W / 2, y, BTN_W, BTN_H).build());
    }

    // Scratch fields returned by fieldRow / singleField
    private TextFieldWidget fieldLeft, fieldRight, fieldSingle;

    /**
     * Places two labelled fields side by side.
     * Left field centred at cx - 36;  right field centred at cx + 36.
     * Labels appear 11px above each field.
     */
    private void fieldRow(String leftLabel, String rightLabel,
                          int cx, int y, int leftVal, int rightVal) {
        int lCx = cx - 36;
        int rCx = cx + 36;
        postLabels.add(new LabelEntry(lCx, y - 11, leftLabel,  C_WHITE, true));
        postLabels.add(new LabelEntry(rCx, y - 11, rightLabel, C_WHITE, true));
        fieldLeft  = mkField(lCx - FIELD_W / 2, y, leftVal);
        fieldRight = mkField(rCx - FIELD_W / 2, y, rightVal);
    }

    /** Places a single centred labelled field. Label appears 11px above the field. */
    private void singleField(String label, int cx, int y, int val) {
        postLabels.add(new LabelEntry(cx, y - 11, label, C_WHITE, true));
        fieldSingle = mkField(cx - FIELD_W / 2, y, val);
    }

    private TextFieldWidget mkField(int x, int y, int value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, FIELD_W, FIELD_H, Text.empty());
        f.setText(String.valueOf(value));
        f.setMaxLength(6);
        f.setTextPredicate(s -> s.matches("-?\\d{0,5}"));
        return addDrawableChild(f);
    }

    private static Text toggleMsg(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "§aON§r" : "§cOFF§r"));
    }

    private static int parseInt(TextFieldWidget f, int fallback) {
        try { return Integer.parseInt(f.getText().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
