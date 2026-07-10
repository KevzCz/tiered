package draylar.tiered.util;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import draylar.tiered.TieredKeybinds;
import draylar.tiered.api.ImprintPlatesData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class ImprintPlatesComponent implements ClientTooltipComponent {

    private static final int HEADER_H = 11;
    private static final int ROW_H = 16;
    private static final int ROW_GAP = 1;
    private static final int TOP_GAP = 2;
    private static final int BOTTOM_GAP = 2;
    private static final int ICON = 16;
    private static final int ICON_GAP = 3;
    private static final int PLATE_H = 12;
    private static final int DETAIL_H = 9;
    private static final int PAD_X = 4;
    private static final int H_GAP = 3;
    private static final int HEADER_GAP = 4;

    private static final int MAX_PLATE_W = 150;

    private static final ResourceLocation RUNE_FRAME =
            ResourceLocation.fromNamespaceAndPath("kevs", "textures/item/rune_frame.png");

    private final Component header;
    private final List<ImprintPlatesData.SlotView> slots;
    private final int emptySlots;
    private final int cachedHeight;

    public ImprintPlatesComponent(ImprintPlatesData data) {
        this.header = data.header();
        this.slots = data.slots();
        this.emptySlots = Math.max(0, data.capacity() - data.slots().size());

        Font tr = Minecraft.getInstance().font;
        this.cachedHeight = tr != null ? computeHeight(tr) : estimateHeight();
    }

    private int plateWidth(Font tr, ImprintPlatesData.Plate plate) {
        return PAD_X + tr.width(plate.label()) + PAD_X;
    }

    private List<List<ImprintPlatesData.Plate>> wrapPlates(Font tr, ImprintPlatesData.SlotView slot) {
        List<List<ImprintPlatesData.Plate>> rows = new ArrayList<>();
        List<ImprintPlatesData.Plate> current = new ArrayList<>();
        int rowWidth = 0;
        for (ImprintPlatesData.Plate plate : slot.plates()) {
            int pw = plateWidth(tr, plate);
            int needed = (current.isEmpty() ? 0 : H_GAP) + pw;
            if (!current.isEmpty() && rowWidth + needed > MAX_PLATE_W) {
                rows.add(current);
                current = new ArrayList<>();
                rowWidth = 0;
            }
            current.add(plate);
            rowWidth += (current.size() == 1 ? 0 : H_GAP) + pw;
        }
        if (!current.isEmpty()) rows.add(current);
        if (rows.isEmpty()) rows.add(new ArrayList<>());
        return rows;
    }

    // Shift-only detail lines (works/scales with), one per plate, rendered below the plates.
    private List<String> detailLines(ImprintPlatesData.SlotView slot) {
        List<String> out = new ArrayList<>();
        if (!TieredKeybinds.worksWithHeld()) return out;
        for (ImprintPlatesData.Plate p : slot.plates()) {
            String d = plateDetail(p);
            if (d != null) out.add(d);
        }
        return out;
    }

    private int slotHeight(Font tr, ImprintPlatesData.SlotView slot) {
        int rows = wrapPlates(tr, slot).size();
        int platesH = rows * PLATE_H + (rows - 1) * ROW_GAP;
        int h = Math.max(ROW_H, platesH);
        int details = detailLines(slot).size();
        if (details > 0) h += details * (DETAIL_H + ROW_GAP);
        return h;
    }

    private int slotRowWidth(Font tr, ImprintPlatesData.SlotView slot) {
        int max = 0;
        for (List<ImprintPlatesData.Plate> row : wrapPlates(tr, slot)) {
            int w = 0;
            for (ImprintPlatesData.Plate p : row) {
                w += (w == 0 ? 0 : H_GAP) + plateWidth(tr, p);
                if (!TieredKeybinds.worksWithHeld() && hasRequirementIcon(p)) w += PLATE_H + 2;
            }
            max = Math.max(max, w);
        }
        for (String d : detailLines(slot)) max = Math.max(max, tr.width(d));
        return ICON + ICON_GAP + max;
    }

    private int headerRowWidth(Font tr) {
        int w = header == null ? 0 : tr.width(header);
        if (emptySlots > 0) w += (w > 0 ? HEADER_GAP : 0) + emptySlots * ICON + (emptySlots - 1) * H_GAP;
        return w;
    }

    private int headerRowHeight() {
        return emptySlots > 0 ? ICON : HEADER_H;
    }

    private int computeHeight(Font tr) {
        int h = TOP_GAP + headerRowHeight();
        if (!slots.isEmpty()) {
            h += ROW_GAP;
            for (int i = 0; i < slots.size(); i++) {
                h += slotHeight(tr, slots.get(i));
                if (i < slots.size() - 1) h += ROW_GAP;
            }
        }
        return h + BOTTOM_GAP;
    }

    private int estimateHeight() {

        int h = TOP_GAP + headerRowHeight();
        if (!slots.isEmpty()) {
            h += ROW_GAP + slots.size() * ROW_H + (slots.size() - 1) * ROW_GAP;
        }
        return h + BOTTOM_GAP;
    }

    @Override
    public int getHeight() {
        return cachedHeight;
    }

    @Override
    public int getWidth(Font tr) {
        int max = headerRowWidth(tr);
        for (ImprintPlatesData.SlotView slot : slots) max = Math.max(max, slotRowWidth(tr, slot));
        return max;
    }

    @Override
    public void renderText(Font tr, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource vc) {
        int headerH = headerRowHeight();
        if (header != null) {
            tr.drawInBatch(header, x, y + TOP_GAP + (headerH - 8) / 2, 0xFFAAAAAA, false, matrix, vc,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
        int ry = y + TOP_GAP + headerH + ROW_GAP;
        for (ImprintPlatesData.SlotView slot : slots) {
            List<List<ImprintPlatesData.Plate>> rows = wrapPlates(tr, slot);
            List<String> details = detailLines(slot);
            int platesBlockH = rows.size() * PLATE_H + (rows.size() - 1) * ROW_GAP;
            int slotH = slotHeight(tr, slot);
            boolean shift = TieredKeybinds.worksWithHeld();

            int platesTopOffset = details.isEmpty() ? (slotH - platesBlockH) / 2 : 0;
            int plateRowY = ry + platesTopOffset;
            for (List<ImprintPlatesData.Plate> row : rows) {
                int px = x + ICON + ICON_GAP;
                int textY = plateRowY + (PLATE_H - 8) / 2;
                for (ImprintPlatesData.Plate plate : row) {
                    int textColor = plate.cooldownFill() >= 1f ? 0xFFFFFFFF
                            : lerpColor(0xFF888888, 0xFFFFFFFF, plate.cooldownFill());
                    tr.drawInBatch(plate.label(), px + PAD_X, textY, textColor, true, matrix, vc,
                            Font.DisplayMode.NORMAL, 0, 0xF000F0);
                    px += plateWidth(tr, plate) + H_GAP;
                    if (!shift && hasRequirementIcon(plate)) px += PLATE_H + 2;
                }
                plateRowY += PLATE_H + ROW_GAP;
            }
            int detailY = ry + platesBlockH + ROW_GAP;
            for (String d : details) {
                tr.drawInBatch(d, x + ICON + ICON_GAP, detailY, 0xFF888888, false, matrix, vc,
                        Font.DisplayMode.NORMAL, 0, 0xF000F0);
                detailY += DETAIL_H + ROW_GAP;
            }
            ry += slotH + ROW_GAP;
        }
    }

    @Override
    public void renderImage(Font tr, int x, int y, GuiGraphics context) {
        RenderSystem.enableBlend();

        int headerH = headerRowHeight();
        if (emptySlots > 0) {
            int hx = x + (header == null ? 0 : tr.width(header) + HEADER_GAP);
            int hy = y + TOP_GAP + (headerH - ICON) / 2;
            for (int i = 0; i < emptySlots; i++) {
                context.blit(RUNE_FRAME, hx, hy, ICON, ICON, 0, 0, 32, 32, 32, 32);
                hx += ICON + H_GAP;
            }
        }

        int ry = y + TOP_GAP + headerH + ROW_GAP;
        for (ImprintPlatesData.SlotView slot : slots) {
            List<List<ImprintPlatesData.Plate>> rows = wrapPlates(tr, slot);
            boolean hasDetails = !detailLines(slot).isEmpty();
            int platesBlockH = rows.size() * PLATE_H + (rows.size() - 1) * ROW_GAP;
            int slotH = slotHeight(tr, slot);
            int platesTopOffset = hasDetails ? 0 : (slotH - platesBlockH) / 2;

            int iconY = hasDetails ? ry : ry + (slotH - ICON) / 2;
            if (slot.rune() != null && !slot.rune().isEmpty()) {
                context.renderItem(slot.rune(), x, iconY);
            } else {
                context.blit(RUNE_FRAME, x, iconY, ICON, ICON, 0, 0, 32, 32, 32, 32);
            }

            RenderSystem.enableBlend();
            int plateRowY = ry + platesTopOffset;
            boolean shift = TieredKeybinds.worksWithHeld();
            for (List<ImprintPlatesData.Plate> row : rows) {
                int px = x + ICON + ICON_GAP;
                for (ImprintPlatesData.Plate plate : row) {
                    int pw = plateWidth(tr, plate);
                    drawPlate(context, px, plateRowY, pw, PLATE_H, plate.fillColor(), plate.cooldownFill(), plate.active());
                    px += pw + H_GAP;
                    if (!shift) px = drawRequirementIcon(context, plate, px, plateRowY);
                }
                plateRowY += PLATE_H + ROW_GAP;
            }
            ry += slotH + ROW_GAP;
        }
        RenderSystem.disableBlend();
    }

    private static boolean hasRequirementIcon(ImprintPlatesData.Plate plate) {
        return plate.imprintId() != null && !plate.imprintId().isEmpty()
                && !ImprintRequirements.icons(plate.imprintId()).isEmpty();
    }

    private static String plateDetail(ImprintPlatesData.Plate plate) {
        if (plate.imprintId() == null || plate.imprintId().isEmpty()) return null;
        List<String> tags = ImprintRequirements.tags(plate.imprintId());
        if (tags.isEmpty() && plate.abilityId() != null && !plate.abilityId().isEmpty()) {
            tags = ImprintRequirements.abilityTags(plate.imprintId(), plate.abilityId());
        }
        if (tags.isEmpty()) return null;
        String works = Component.translatable("tiered.imprint.works_with").getString()
                + ": " + String.join(", ", tags);
        String label = plate.label() == null ? "" : plate.label().getString();
        return label.isEmpty() ? works : label + " — " + works;
    }

    private static int drawRequirementIcon(GuiGraphics context, ImprintPlatesData.Plate plate, int px, int plateRowY) {
        if (plate.imprintId() == null || plate.imprintId().isEmpty()) return px;
        List<ItemStack> icons = ImprintRequirements.icons(plate.imprintId());
        if (icons.isEmpty()) return px;
        long t = System.currentTimeMillis() / 900L;
        ItemStack icon = icons.get((int) (t % icons.size()));
        int size = PLATE_H;
        int iy = plateRowY + (PLATE_H - size) / 2;
        var matrices = context.pose();
        matrices.pushPose();
        matrices.translate(px, iy, 0);
        float scale = size / 16f;
        matrices.scale(scale, scale, 1f);
        context.renderItem(icon, 0, 0);
        matrices.popPose();
        return px + size + 2;
    }

    public static int plateHeight() { return PLATE_H; }
    public static int padX() { return PAD_X; }

    public static int drawStandalonePlate(GuiGraphics context, Font tr, int x, int y, Component label, int fill) {
        int w = PAD_X + tr.width(label) + PAD_X;
        RenderSystem.enableBlend();
        drawPlate(context, x, y, w, PLATE_H, fill, 1.0f, false);
        RenderSystem.disableBlend();
        int textY = y + (PLATE_H - 8) / 2;
        context.drawString(tr, label, x + PAD_X, textY, 0xFFFFFFFF, true);
        return w;
    }

    private static void drawPlate(GuiGraphics context, int x, int y, int w, int h, int fill, float cooldownFill,
            boolean active) {

        if (active) {
            float pulse = 0.5f + 0.5f * (float) Math.sin((System.currentTimeMillis() % 1400L) / 1400.0 * Math.PI * 2);
            fill = PlateColors.lighten(fill, 0.10f + pulse * 0.18f);
        }
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

        if (active) {
            float pulse = 0.5f + 0.5f * (float) Math.sin((System.currentTimeMillis() % 1400L) / 1400.0 * Math.PI * 2);
            int a = (int) (90 + pulse * 120) & 0xFF;
            int glow = (a << 24) | (fill & 0xFFFFFF);
            context.fill(x, y - 1, x + w, y, glow);
            context.fill(x, y + h, x + w, y + h + 1, glow);
            context.fill(x - 1, y, x, y + h, glow);
            context.fill(x + w, y, x + w + 1, y + h, glow);
        }

        if (cooldownFill < 1f) {
            float clamped = Math.max(0f, Math.min(1f, cooldownFill));
            int filledPx = (int) (clamped * (w - 2));
            int overlayX = x + 1 + filledPx;
            int overlayW = (w - 2) - filledPx;
            if (overlayW > 0) {
                context.fill(overlayX, y + 1, overlayX + overlayW, y + h - 1, 0xAA000000);
            }
        }
    }

    private static int lerpColor(int a, int b, float t) {
        int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = aa + (int) ((ba - aa) * t);
        int rr = ar + (int) ((br - ar) * t);
        int rg = ag + (int) ((bg - ag) * t);
        int rb = ab + (int) ((bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}
