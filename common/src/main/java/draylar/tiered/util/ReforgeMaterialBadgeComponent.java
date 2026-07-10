package draylar.tiered.util;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import draylar.tiered.api.ReforgeMaterialBadgeData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class ReforgeMaterialBadgeComponent implements ClientTooltipComponent {

    private static final int PAD_X = 5;
    private static final int BADGE_H = 14;
    private static final int BADGE_GAP = 3;
    private static final int TOP_GAP = 2;
    private static final int BOTTOM_GAP = 3;
    private static final int GLYPH_GAP = 3;
    private static final int LORE_GAP = 2;
    private static final int LINE_H = 10;
    private static final int MIN_WIDTH = 60;

    private static final int DESC_H = 10;

    private final List<ReforgeMaterialBadgeData.Badge> badges;
    private final List<Component> lore;
    private final boolean showDescriptions;
    private final boolean compact;

    public ReforgeMaterialBadgeComponent(ReforgeMaterialBadgeData data) {
        this.badges = data.badges();
        this.lore = data.lore() == null ? List.of() : data.lore();
        this.showDescriptions = data.showDescriptions();
        this.compact = data.compact();
    }

    private int topGap() {
        return compact ? 1 : TOP_GAP;
    }

    private int bottomGap() {
        return compact ? 1 : BOTTOM_GAP;
    }

    private boolean hasDesc(ReforgeMaterialBadgeData.Badge badge) {
        return !compact && showDescriptions && badge.description() != null;
    }

    private int descBlockHeight() {
        int h = 0;
        for (ReforgeMaterialBadgeData.Badge badge : badges) {
            if (hasDesc(badge)) h += DESC_H;
        }
        return h;
    }

    private int badgeContentWidth(Font tr, ReforgeMaterialBadgeData.Badge badge) {
        int glyphW = badge.glyph().isEmpty() ? 0 : tr.width(badge.glyph()) + GLYPH_GAP;
        return PAD_X + glyphW + tr.width(badge.label()) + PAD_X;
    }

    @Override
    public int getHeight() {
        int h = 0;
        if (!badges.isEmpty()) {
            h += topGap() + badges.size() * BADGE_H + (badges.size() - 1) * BADGE_GAP + bottomGap();
            h += descBlockHeight();
        }
        if (!lore.isEmpty()) {
            h += LORE_GAP + lore.size() * LINE_H;
        }
        return h;
    }

    @Override
    public int getWidth(Font textRenderer) {
        int max = MIN_WIDTH;
        for (ReforgeMaterialBadgeData.Badge badge : badges) {
            max = Math.max(max, badgeContentWidth(textRenderer, badge));
            if (hasDesc(badge)) {
                max = Math.max(max, PAD_X + textRenderer.width(badge.description()));
            }
        }
        for (Component line : lore) {
            max = Math.max(max, textRenderer.width(line));
        }
        return max;
    }

    @Override
    public void renderText(Font textRenderer, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource vertexConsumers) {
        int by = y + topGap();
        for (ReforgeMaterialBadgeData.Badge badge : badges) {
            int textY = by + (BADGE_H - 8) / 2;
            int tx = x + PAD_X;
            if (!badge.glyph().isEmpty()) {
                textRenderer.drawInBatch(Component.literal(badge.glyph()), tx, textY, 0xFFFFFFFF, true, matrix, vertexConsumers,
                        Font.DisplayMode.NORMAL, 0, 0xF000F0);
                tx += textRenderer.width(badge.glyph()) + GLYPH_GAP;
            }
            textRenderer.drawInBatch(badge.label(), tx, textY, 0xFFFFFFFF, true, matrix, vertexConsumers,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
            by += BADGE_H + BADGE_GAP;
            if (hasDesc(badge)) {
                textRenderer.drawInBatch(badge.description(), x + PAD_X, by, 0xFFAAAAAA, false, matrix, vertexConsumers,
                        Font.DisplayMode.NORMAL, 0, 0xF000F0);
                by += DESC_H;
            }
        }

        int ly = y + (badges.isEmpty() ? 0 : topGap() + badges.size() * BADGE_H + (badges.size() - 1) * BADGE_GAP + bottomGap() + descBlockHeight()) + LORE_GAP;
        for (Component line : lore) {
            textRenderer.drawInBatch(line, x, ly, 0xFFAAAAAA, false, matrix, vertexConsumers,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
            ly += LINE_H;
        }
    }

    @Override
    public void renderImage(Font textRenderer, int x, int y, GuiGraphics context) {
        RenderSystem.enableBlend();
        int fullWidth = getWidth(textRenderer);
        int by = y + topGap();
        for (ReforgeMaterialBadgeData.Badge badge : badges) {
            drawPlate(context, x, by, fullWidth, BADGE_H, badge.fillColor());
            by += BADGE_H + BADGE_GAP;
            if (hasDesc(badge)) by += DESC_H;
        }
        RenderSystem.disableBlend();
    }

    private static void drawPlate(GuiGraphics context, int x, int y, int w, int h, int fill) {
        int top = PlateColors.lighten(fill, 0.18f);
        int bottom = PlateColors.darken(fill, 0.12f);
        int border = PlateColors.darken(fill, 0.45f);
        int shadow = 0x55000000;

        context.fill(x + 2, y + h, x + w, y + h + 1, shadow);
        context.fill(x + w, y + 2, x + w + 1, y + h, shadow);

        context.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
        context.fillGradient(x, y + 2, x + 1, y + h - 2, top, bottom);
        context.fillGradient(x + w - 1, y + 2, x + w, y + h - 2, top, bottom);

        context.fill(x + 1, y, x + w - 1, y + 1, border);
        context.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
        context.fill(x, y + 2, x + 1, y + h - 2, border);
        context.fill(x + w - 1, y + 2, x + w, y + h - 2, border);

        context.fill(x + 2, y + 1, x + w - 2, y + 2, PlateColors.lighten(fill, 0.35f));

        if (fill != 0xFF444444) {
            PlateColors.drawShine(context, x + 1, y + 1, w - 2, h - 2);
        }
    }
}
