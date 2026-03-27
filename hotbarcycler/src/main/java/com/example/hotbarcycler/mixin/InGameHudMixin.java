package com.example.hotbarcycler.mixin;

import com.example.hotbarcycler.HotbarCyclerClient;
import com.example.hotbarcycler.HotbarCyclerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Unique private static final int HOTBAR_HALF_W = 91;
    @Unique private static final int HOTBAR_HEIGHT = 22;
    @Unique private static final int IND_ROWS      = 4;
    @Unique private static final int IND_GAP       = 2;
    @Unique private static final int IND_MARGIN    = 4;
    @Unique private static final int PREV_SLOT_GAP = 2;

    // Shared transparent-inventory palette used by ALL three HUD elements
    @Unique private static final int OUTLINE_DIM    = 0x55FFFFFF; // 33% white — normal slots
    @Unique private static final int OUTLINE_MID    = 0x88FFFFFF; // 53% white — hotbar row / brighter
    @Unique private static final int OUTLINE_BRIGHT = 0xAAFFFFFF; // 67% white — accent / active
    @Unique private static final int SELECTED_FILL  = 0x3300AAFF; // blue tint fill
    @Unique private static final int SELECTED_EDGE  = 0xBB00AAFF; // blue outline
    @Unique private static final int SEP_LINE       = 0x44FFFFFF; // separator

    @Unique
    private static int darkWithAlpha(int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        if (a == 0) return 0;
        return (a << 24) | 0x1B1B1B;
    }

    /** Draws a 1-px rectangular outline using four edge fills. */
    @Unique
    private static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w, y + 1,     color); // top
        ctx.fill(x,         y + h - 1, x + w, y + h,     color); // bottom
        ctx.fill(x,         y,         x + 1, y + h,     color); // left
        ctx.fill(x + w - 1, y,         x + w, y + h,     color); // right
    }

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void hotbarcycler$onTail(DrawContext ctx,
                                     RenderTickCounter tickCounter,
                                     CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int hotbarLeft = sw / 2 - HOTBAR_HALF_W;
        int hotbarTop  = sh - HOTBAR_HEIGHT;
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();

        if (HotbarCyclerClient.isColumnPreviewVisible())
            hotbarcycler$drawColumnPreview(ctx, client, sw, sh, cfg);
        if (HotbarCyclerClient.isRowIndicatorVisible())
            hotbarcycler$drawRowIndicator(ctx, hotbarLeft, hotbarTop, cfg);
        if (HotbarCyclerClient.isInventoryOverlayVisible())
            hotbarcycler$drawInventoryOverlay(ctx, client, sw, sh, cfg);
    }

    // =========================================================================
    // Column Preview
    // Three thin-outline slots below the hotbar.
    // Rightmost slot (row3) is accent-bright — it's what column_prev pulls in.
    // =========================================================================
    @Unique
    private void hotbarcycler$drawColumnPreview(DrawContext ctx, MinecraftClient client,
                                                int sw, int sh, HotbarCyclerConfig cfg) {
        int sel    = client.player.getInventory().selectedSlot;
        ItemStack r1 = client.player.getInventory().getStack(sel +  9);
        ItemStack r2 = client.player.getInventory().getStack(sel + 18);
        ItemStack r3 = client.player.getInventory().getStack(sel + 27);

        if (r1.isEmpty() && r2.isEmpty() && r3.isEmpty() && !cfg.showEmptyColumns) return;

        int slotW  = Math.max(16, cfg.columnPreviewSlotWidth);
        int slotH  = Math.max(16, cfg.columnPreviewSlotHeight);
        int bg     = darkWithAlpha(cfg.columnPreviewAlpha);
        int stripW = 3 * slotW + 2 * PREV_SLOT_GAP;
        int left   = sw / 2 - stripW / 2 + cfg.columnPreviewOffsetX;
        int top    = sh - slotH + cfg.columnPreviewOffsetY;

        ItemStack[] stacks = { r1, r2, r3 };
        for (int i = 0; i < 3; i++) {
            int x = left + i * (slotW + PREV_SLOT_GAP);
            int y = top;

            if (bg != 0) ctx.fill(x, y, x + slotW, y + slotH, bg);

            // Rightmost slot = accent only when it actually has an item to pull in
            int edge = (i == 2 && !r3.isEmpty()) ? OUTLINE_BRIGHT : OUTLINE_DIM;
            outline(ctx, x, y, slotW, slotH, edge);

            if (!stacks[i].isEmpty()) {
                int ix = x + (slotW - 16) / 2;
                int iy = y + (slotH - 16) / 2;
                ctx.drawItemWithoutEntity(stacks[i], ix, iy);
                ctx.drawItemInSlot(client.textRenderer, stacks[i], x, y);            }
        }
    }

    // =========================================================================
    // Row Indicator
    // Four thin-outline squares to the left of the hotbar.
    // Active square = bright solid white; inactive = dim outline only, no fill.
    // =========================================================================
    @Unique
    private void hotbarcycler$drawRowIndicator(DrawContext ctx,
                                               int hotbarLeft, int hotbarTop,
                                               HotbarCyclerConfig cfg) {
        int sqW    = Math.max(1, cfg.rowIndicatorSquareW);
        int sqH    = Math.max(1, cfg.rowIndicatorSquareH);
        int stackH = IND_ROWS * sqH + (IND_ROWS - 1) * IND_GAP;
        int top    = hotbarTop + (HOTBAR_HEIGHT - stackH) / 2 + cfg.rowIndicatorOffsetY;
        int sqR    = hotbarLeft - IND_MARGIN + cfg.rowIndicatorOffsetX;
        int sqL    = sqR - sqW;
        int active = HotbarCyclerClient.getCurrentPage();

        for (int i = 0; i < IND_ROWS; i++) {
            int page = (IND_ROWS - 1) - i;
            int t    = top + i * (sqH + IND_GAP);
            int b    = t + sqH;

            if (page == active) {
                // Active square: bright solid fill + bright outline
                ctx.fill(sqL, t, sqR, b, OUTLINE_BRIGHT);
                outline(ctx, sqL, t, sqW, sqH, OUTLINE_BRIGHT);
            } else {
                // Inactive squares: dim outline only, no fill (world shows through)
                outline(ctx, sqL, t, sqW, sqH, OUTLINE_DIM);
            }
        }
    }

    // =========================================================================
    // Inventory Overlay
    // Transparent-inventory style: thin outlines, no label, no panel background.
    // =========================================================================
    @Unique
    private void hotbarcycler$drawInventoryOverlay(DrawContext ctx, MinecraftClient client,
                                                   int sw, int sh, HotbarCyclerConfig cfg) {
        int slotSize      = Math.max(16, cfg.inventoryOverlaySlotSize);
        int slotsPerRow   = Math.max(1,  Math.min(9, cfg.inventoryOverlaySlotsPerRow));
        int hotbarPadding = Math.max(0,  cfg.inventoryOverlayHotbarPadding);
        int bg            = darkWithAlpha(cfg.inventoryOverlayAlpha);

        int pad     = 6;
        int gap     = 2;
        int invRows = 3;

        int panelW = pad * 2 + slotsPerRow * slotSize + (slotsPerRow - 1) * gap;
        int panelH = pad * 2
                + invRows * slotSize + (invRows - 1) * gap
                + hotbarPadding
                + slotSize
                + pad;

        int panelX = sw / 2 - panelW / 2 + cfg.inventoryOverlayOffsetX;
        int panelY = sh / 2 - panelH / 2 + cfg.inventoryOverlayOffsetY;

        // Optional dark panel background (0 = skip = transparent)
        if (bg != 0) ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, bg);

        // Separator between inventory rows and hotbar row
        if (hotbarPadding >= 2) {
            int sepY = panelY + pad + invRows * slotSize + (invRows - 1) * gap + hotbarPadding / 2;
            ctx.fill(panelX + pad, sepY, panelX + panelW - pad, sepY + 1, SEP_LINE);
        }

        int sel = client.player.getInventory().selectedSlot;

        // Inventory rows: slots 9-35, top=row1(9-17), mid=row2(18-26), bot=row3(27-35)
        for (int invSlot = 9; invSlot < 36; invSlot++) {
            int li  = invSlot - 9;
            int row = li / 9;
            int col = li % 9;
            if (col >= slotsPerRow) continue;

            int x = panelX + pad + col * (slotSize + gap);
            int y = panelY + pad + row * (slotSize + gap);
            hotbarcycler$drawSlot(ctx, client, x, y, slotSize, bg, OUTLINE_DIM, invSlot, false);
        }

        // Hotbar row at bottom (slightly brighter outlines)
        int hotbarRowY = panelY + pad + invRows * slotSize + (invRows - 1) * gap + hotbarPadding;
        for (int col = 0; col < slotsPerRow && col < 9; col++) {
            int x = panelX + pad + col * (slotSize + gap);
            hotbarcycler$drawSlot(ctx, client, x, hotbarRowY, slotSize,
                    bg, OUTLINE_MID, col, col == sel);
        }
    }

    @Unique
    private void hotbarcycler$drawSlot(DrawContext ctx, MinecraftClient client,
                                       int x, int y, int slotSize,
                                       int bg, int edgeColor,
                                       int slotIndex, boolean selected) {
        // Optional dark fill
        if (bg != 0) ctx.fill(x, y, x + slotSize, y + slotSize, bg);

        // Selected: blue tint + brighter border
        if (selected) {
            ctx.fill(x, y, x + slotSize, y + slotSize, SELECTED_FILL);
            edgeColor = SELECTED_EDGE;
        }

        // Thin 1-px outline
        outline(ctx, x, y, slotSize, slotSize, edgeColor);

        // Item centred inside
        ItemStack stack = client.player.getInventory().getStack(slotIndex);
        if (!stack.isEmpty()) {
            ctx.drawItemWithoutEntity(stack, x + (slotSize - 16) / 2, y + (slotSize - 16) / 2);
//            ctx.(client.textRenderer, stack,
//                    x + (slotSize - 16) / 2, y + (slotSize - 16) / 2);
            ctx.drawItemInSlot(client.textRenderer, stack, x + (slotSize - 16) / 2, y + (slotSize - 16) / 2);
        }
    }
}
