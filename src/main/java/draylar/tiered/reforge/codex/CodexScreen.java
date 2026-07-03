package draylar.tiered.reforge.codex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import draylar.tiered.util.ImprintPlatesComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CodexScreen extends Screen {

    private static final int PANEL_FILL   = 0xF0140A1C;
    private static final int PANEL_TOP    = 0xF01E1230;
    private static final int BORDER       = 0xFF6A4A8A;
    private static final int BORDER_HOVER = 0xFFB088E0;
    private static final int ACCENT       = 0xFFE0C8FF;
    private static final int SUBTLE       = 0xFFA890C0;
    private static final int FIELD_LABEL  = 0xFF8A78A8;
    private static final int FIELD_VALUE  = 0xFFD8C8E8;
    private static final int DESC_COLOR   = 0xFFBFAFD8;
    private static final int TAB_ACTIVE   = 0xFF4A2E6E;
    private static final int TAB_IDLE     = 0xF0241830;

    private static final int PANEL_W   = 320;
    private static final int PANEL_PAD = 10;
    private static final int HEADER_H  = 30;
    private static final int FOOTER_H  = 26;
    private static final int TAB_H     = 16;
    private static final int SEARCH_H  = 14;
    private static final int ROW_PAD   = 6;
    private static final int LINE_H    = 11;
    private static final int ICON      = 16;
    private static final int SCROLL_W  = 4;
    private static final int SCROLL_GAP = 5;

    private final Screen parent;
    private final List<CodexTab> tabs;

    private int activeTab = 0;
    private List<CodexEntry> allEntries = List.of();
    private List<CodexEntry> entries = List.of();
    @Nullable
    private TextFieldWidget searchField;

    private int panelX, panelY, panelH, listTop, listBottom, listLeft, listRight;
    private int scroll = 0;
    private int contentHeight = 0;
    private boolean draggingScrollbar = false;
    @Nullable
    private ItemStack hoveredItem;

    private final List<int[]> worksWithHits = new ArrayList<>();
    private final List<CodexEntry.WorksWithTag> worksWithHitTags = new ArrayList<>();
    @Nullable
    private CodexEntry.WorksWithTag openTag;
    private int tagPanelScroll = 0;

    public CodexScreen(Screen parent) {
        super(Text.translatable("screen.tiered.codex.title"));
        this.parent = parent;
        this.tabs = CodexTabRegistry.tabs();
    }

    @Override
    protected void init() {
        panelH = Math.min(this.height - 40, 230);
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - panelH) / 2;

        int searchY = panelY + HEADER_H + TAB_H + 2;
        listTop = searchY + SEARCH_H + 3;
        listBottom = panelY + panelH - FOOTER_H;
        listLeft = panelX + PANEL_PAD;

        listRight = panelX + PANEL_W - PANEL_PAD - SCROLL_W - SCROLL_GAP;

        String previousQuery = searchField != null ? searchField.getText() : "";
        searchField = new TextFieldWidget(this.textRenderer, panelX + PANEL_PAD, searchY,
                PANEL_W - PANEL_PAD * 2, SEARCH_H, Text.translatable("screen.tiered.codex.search"));
        searchField.setMaxLength(64);
        searchField.setPlaceholder(Text.translatable("screen.tiered.codex.search").styled(s -> s.withColor(Formatting.DARK_GRAY)));
        searchField.setText(previousQuery);
        searchField.setChangedListener(q -> applyFilter());
        this.addDrawableChild(searchField);

        loadActiveTab();

        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("gui.done"), b -> close())
                .dimensions(panelX + PANEL_W / 2 - 50, panelY + panelH - FOOTER_H + 3, 100, 18)
                .build());
    }

    private void loadActiveTab() {
        allEntries = tabs.isEmpty() ? List.of() : tabs.get(activeTab).buildEntries();
        applyFilter();
    }

    private void applyFilter() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            entries = allEntries;
        } else {
            List<CodexEntry> out = new ArrayList<>();
            for (CodexEntry e : allEntries) {
                if (matches(e, query)) out.add(e);
            }
            entries = out;
        }
        scroll = 0;
        contentHeight = 0;
        int contentW = listRight - listLeft;
        for (CodexEntry e : entries) contentHeight += rowHeight(e, contentW);
    }

    private static boolean matches(CodexEntry e, String query) {
        if (query.startsWith("group:")) {
            String g = query.substring("group:".length()).trim();
            if (g.isEmpty()) return true;
            for (String[] field : e.fields()) {
                if ("Group".equalsIgnoreCase(field[0]) && field[1] != null
                        && field[1].toLowerCase(Locale.ROOT).contains(g)) return true;
            }
            return false;
        }
        if (e.name() != null && e.name().toLowerCase(Locale.ROOT).contains(query)) return true;
        if (e.description() != null && e.description().toLowerCase(Locale.ROOT).contains(query)) return true;
        for (String[] field : e.fields()) {
            for (String part : field) {
                if (part != null && part.toLowerCase(Locale.ROOT).contains(query)) return true;
            }
        }

        for (ItemStack stack : e.relatedItems()) {
            if (stack.getName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (openTag != null) {
            int[] b = tagPanelBounds();
            boolean inside = mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3];
            if (!inside) { openTag = null; tagPanelScroll = 0; }
            return true;
        }
        if (button == 0) {
            for (int i = 0; i < worksWithHits.size(); i++) {
                int[] r = worksWithHits.get(i);
                if (mouseX >= r[0] && mouseX <= r[2] && mouseY >= r[1] && mouseY <= r[3]) {
                    openTag = worksWithHitTags.get(i);
                    tagPanelScroll = 0;
                    if (this.client != null) this.client.getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
                    return true;
                }
            }

            int tabW = (PANEL_W - PANEL_PAD * 2) / Math.max(1, tabs.size());
            int tabY = panelY + HEADER_H - 4;
            for (int i = 0; i < tabs.size(); i++) {
                int tx = panelX + PANEL_PAD + i * tabW;
                if (mouseX >= tx && mouseX <= tx + tabW - 2 && mouseY >= tabY && mouseY <= tabY + TAB_H) {
                    if (i != activeTab) { activeTab = i; loadActiveTab(); }
                    if (this.client != null) this.client.getSoundManager().play(
                            PositionedSoundInstance.master(
                                    SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
                    return true;
                }
            }

            if (overScrollbar(mouseX, mouseY)) {
                draggingScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingScrollbar && button == 0) {
            int viewH = listBottom - listTop;
            int maxScroll = Math.max(0, contentHeight - viewH);

            float frac = (float) (mouseY - listTop) / Math.max(1, viewH);
            scroll = Math.max(0, Math.min(maxScroll, Math.round(frac * maxScroll)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (openTag != null) {
            int[] b = tagPanelBounds();
            int cols = b[4], gridH = b[5];
            int rows = (openTag.items().size() + cols - 1) / cols;
            int maxScroll = Math.max(0, rows * TAG_ICON - gridH);
            tagPanelScroll = Math.max(0, Math.min(maxScroll, tagPanelScroll - (int) (vertical * 16)));
            return true;
        }
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (vertical * 16)));
        return true;
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        int viewH = listBottom - listTop;
        if (contentHeight <= viewH) return false;
        int barX = panelX + PANEL_W - PANEL_PAD - SCROLL_W;
        return mouseX >= barX && mouseX <= barX + SCROLL_W && mouseY >= listTop && mouseY <= listBottom;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x88000000);

        context.fillGradient(panelX, panelY, panelX + PANEL_W, panelY + panelH, PANEL_TOP, PANEL_FILL);
        context.drawBorder(panelX, panelY, PANEL_W, panelH, BORDER);

        int cx = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, panelY + 8, ACCENT);

        drawTabs(context, mouseX, mouseY);

        hoveredItem = null;
        worksWithHits.clear();
        worksWithHitTags.clear();
        boolean modal = openTag != null;

        context.enableScissor(panelX + 1, listTop, panelX + PANEL_W - 1, listBottom);
        int contentW = listRight - listLeft;
        int y = listTop - scroll;
        for (CodexEntry e : entries) {
            int h = rowHeight(e, contentW);
            if (y + h >= listTop && y <= listBottom) {
                drawRow(context, e, listLeft, y, contentW, h, modal ? -1 : mouseX, modal ? -1 : mouseY);
            }
            y += h;
        }
        context.disableScissor();

        drawScrollbar(context);

        if (!modal && hoveredItem != null && mouseY >= listTop && mouseY <= listBottom) {
            context.drawTooltip(this.textRenderer,
                    Screen.getTooltipFromItem(this.client, hoveredItem),
                    mouseX, mouseY);
        }

        if (entries.isEmpty()) {
            boolean searching = searchField != null && !searchField.getText().trim().isEmpty();
            Text msg = Text.translatable(searching ? "screen.tiered.codex.no_results" : "screen.tiered.codex.empty")
                    .styled(s -> s.withColor(Formatting.GRAY));
            context.drawCenteredTextWithShadow(this.textRenderer, msg, cx, listTop + 10, SUBTLE);
        }

        if (modal) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 400);
            drawTagPanel(context, mouseX, mouseY);
            context.getMatrices().pop();
        }
    }

    private static final int TAG_PANEL_W = 176;
    private static final int TAG_ICON = 18;

    private int[] tagPanelBounds() {
        int cols = (TAG_PANEL_W - PANEL_PAD * 2) / TAG_ICON;
        int rows = openTag == null ? 0 : (openTag.items().size() + cols - 1) / cols;
        int gridH = Math.min(rows, 6) * TAG_ICON;
        int h = HEADER_H + gridH + PANEL_PAD;
        int px = (this.width - TAG_PANEL_W) / 2;
        int py = (this.height - h) / 2;
        return new int[]{px, py, TAG_PANEL_W, h, cols, Math.min(rows, 6) * TAG_ICON};
    }

    private void drawTagPanel(DrawContext context, int mouseX, int mouseY) {
        context.fill(0, 0, this.width, this.height, 0x99000000);
        int[] b = tagPanelBounds();
        int px = b[0], py = b[1], pw = b[2], ph = b[3], cols = b[4], gridH = b[5];
        context.fillGradient(px, py, px + pw, py + ph, PANEL_TOP, PANEL_FILL);
        context.drawBorder(px, py, pw, ph, BORDER_HOVER);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(openTag.label()),
                px + pw / 2, py + 6, ACCENT);

        int gridTop = py + HEADER_H;
        int gx0 = px + PANEL_PAD;
        context.enableScissor(px + 1, gridTop, px + pw - 1, gridTop + gridH);
        ItemStack hov = null;
        int i = 0;
        for (ItemStack stack : openTag.items()) {
            int col = i % cols, row = i / cols;
            int ix = gx0 + col * TAG_ICON;
            int iy = gridTop + row * TAG_ICON - tagPanelScroll;
            if (iy + TAG_ICON >= gridTop && iy <= gridTop + gridH) {
                context.drawItem(stack, ix, iy);
                if (mouseX >= ix && mouseX <= ix + 16 && mouseY >= iy && mouseY <= iy + 16) hov = stack;
            }
            i++;
        }
        context.disableScissor();
        if (hov != null) context.drawTooltip(this.textRenderer,
                Screen.getTooltipFromItem(this.client, hov), mouseX, mouseY);
    }

    private void drawTabs(DrawContext context, int mouseX, int mouseY) {
        if (tabs.isEmpty()) return;
        int tabW = (PANEL_W - PANEL_PAD * 2) / tabs.size();
        int tabY = panelY + HEADER_H - 4;
        for (int i = 0; i < tabs.size(); i++) {
            int tx = panelX + PANEL_PAD + i * tabW;
            boolean active = i == activeTab;
            boolean hovered = mouseX >= tx && mouseX <= tx + tabW - 2 && mouseY >= tabY && mouseY <= tabY + TAB_H;
            context.fill(tx, tabY, tx + tabW - 2, tabY + TAB_H, active ? TAB_ACTIVE : TAB_IDLE);
            context.drawBorder(tx, tabY, tabW - 2, TAB_H, active || hovered ? BORDER_HOVER : BORDER);
            Text label = tabs.get(i).title();
            int lw = this.textRenderer.getWidth(label);
            context.drawText(this.textRenderer, label, tx + (tabW - 2 - lw) / 2, tabY + 4,
                    active ? 0xFFFFFFFF : ACCENT, false);
        }
    }

    private int drawGlyph(DrawContext context, String glyph, int x, int plateY) {
        int gy = plateY + (ImprintPlatesComponent.plateHeight() - 8) / 2;
        context.drawText(this.textRenderer, Text.literal(glyph), x + 2, gy, 0xFFCCCCCC, false);
        return x + 2 + this.textRenderer.getWidth(glyph) + 3;
    }

    private void drawRow(DrawContext context, CodexEntry e, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x - 2 && mouseX <= x + w + 2 && mouseY >= y && mouseY <= y + h;
        context.fill(x - 2, y, x + w + 2, y + h, hovered ? 0x55241830 : 0x33140A1C);
        context.drawBorder(x - 2, y, w + 4, h, hovered ? BORDER_HOVER : 0xFF3A2A55);

        int leftW = (w * 6) / 10;
        int rightX = x + leftW + 6;

        int py = y + ROW_PAD;
        ImprintPlatesComponent.drawStandalonePlate(context, this.textRenderer, x + 2, py,
                Text.literal(e.name().toUpperCase(Locale.ROOT)), e.plateFill());
        int fy = py + ImprintPlatesComponent.plateHeight() + 2;
        if (e.description() != null) {
            for (var line : this.textRenderer.wrapLines(Text.literal(e.description()), leftW - 4)) {
                context.drawText(this.textRenderer, line, x + 4, fy, DESC_COLOR, false);
                fy += LINE_H;
            }
            fy += 1;
        }
        for (String[] field : e.fields()) {
            context.drawText(this.textRenderer, Text.literal(field[0] + ":"), x + 4, fy, FIELD_LABEL, false);
            int valX = x + 4 + this.textRenderer.getWidth(field[0] + ": ") + 2;
            context.drawText(this.textRenderer, Text.literal(field[1]), valX, fy, FIELD_VALUE, false);
            fy += LINE_H;
        }

        for (CodexEntry.AbilityRecipe r : e.recipes()) {
            fy += 1;
            int rx = x + 4;
            rx += ImprintPlatesComponent.drawStandalonePlate(context, this.textRenderer, rx, fy,
                    Text.literal(r.gateName().toUpperCase(Locale.ROOT)), r.gateFill());
            rx = drawGlyph(context, "+", rx, fy);
            rx += ImprintPlatesComponent.drawStandalonePlate(context, this.textRenderer, rx, fy,
                    Text.literal(r.selfName().toUpperCase(Locale.ROOT)), r.selfFill());
            rx = drawGlyph(context, "=", rx, fy);
            ImprintPlatesComponent.drawStandalonePlate(context, this.textRenderer, rx, fy,
                    Text.literal(r.abilityName().toUpperCase(Locale.ROOT)), r.abilityFill());
            fy += ImprintPlatesComponent.plateHeight() + 2;
            if (r.description() != null) {
                for (var line : this.textRenderer.wrapLines(Text.literal(r.description()), leftW - 8)) {
                    context.drawText(this.textRenderer, line, x + 8, fy, DESC_COLOR, false);
                    fy += LINE_H;
                }
            }
            fy += 2;
        }

        if (!e.worksWith().isEmpty()) {
            context.drawText(this.textRenderer, Text.translatable("tiered.imprint.works_with"),
                    x + 4, fy, SUBTLE, false);
            int gx = x + 4 + this.textRenderer.getWidth(Text.translatable("tiered.imprint.works_with")) + 6;
            for (CodexEntry.WorksWithTag ww : e.worksWith()) {
                context.drawItem(ww.icon(), gx, fy - 4);
                boolean hov = mouseX >= gx && mouseX <= gx + ICON && mouseY >= fy - 4 && mouseY <= fy - 4 + ICON;
                if (hov) {
                    context.drawBorder(gx - 1, fy - 5, ICON + 2, ICON + 2, BORDER_HOVER);
                    hoveredItem = ww.icon();
                }
                worksWithHits.add(new int[]{gx, fy - 4, gx + ICON, fy - 4 + ICON});
                worksWithHitTags.add(ww);
                gx += ICON + 2;
            }
            fy += ICON;
        }

        if (!e.relatedItems().isEmpty()) {
            int ry = y + ROW_PAD;
            if (e.relatedHeader() != null) {
                context.drawText(this.textRenderer, e.relatedHeader().copy().styled(s -> s.withColor(Formatting.GRAY)),
                        rightX, ry, SUBTLE, false);
                ry += LINE_H;
            }
            int gx = rightX, gy = ry;
            int perRow = Math.max(1, (x + w - rightX) / (ICON + 2));
            int placed = 0;
            for (ItemStack stack : e.relatedItems()) {
                context.drawItem(stack, gx, gy);
                if (mouseX >= gx && mouseX <= gx + ICON && mouseY >= gy && mouseY <= gy + ICON) {
                    hoveredItem = stack;
                }
                gx += ICON + 2;
                if (++placed % perRow == 0) { gx = rightX; gy += ICON + 2; }
            }
        }
    }

    private void drawScrollbar(DrawContext context) {
        int viewH = listBottom - listTop;
        if (contentHeight <= viewH) return;
        int trackX = panelX + PANEL_W - PANEL_PAD - SCROLL_W;
        int barH = Math.max(12, viewH * viewH / contentHeight);
        int maxScroll = contentHeight - viewH;
        int barY = listTop + (maxScroll == 0 ? 0 : (viewH - barH) * scroll / maxScroll);
        context.fill(trackX, listTop, trackX + SCROLL_W, listBottom, 0x40FFFFFF);
        context.fill(trackX, barY, trackX + SCROLL_W, barY + barH, draggingScrollbar ? 0xFFFFFFFF : BORDER_HOVER);
    }

    private int rowHeight(CodexEntry e, int contentW) {
        int leftW = (contentW * 6) / 10;
        int descLines = e.description() == null ? 0
                : this.textRenderer.wrapLines(Text.literal(e.description()), leftW - 4).size();
        int recipeH = 0;
        for (CodexEntry.AbilityRecipe r : e.recipes()) {
            int rl = r.description() == null ? 0
                    : this.textRenderer.wrapLines(Text.literal(r.description()), leftW - 8).size();
            recipeH += 1 + ImprintPlatesComponent.plateHeight() + 2 + rl * LINE_H + 2;
        }
        int leftH = ROW_PAD + ImprintPlatesComponent.plateHeight() + 2
                + (descLines > 0 ? descLines * LINE_H + 1 : 0)
                + e.fields().size() * LINE_H
                + recipeH
                + (e.worksWith().isEmpty() ? 0 : ICON)
                + ROW_PAD;

        if (e.relatedItems().isEmpty()) return leftH;
        int rightX = listLeft + leftW + 6;
        int perRow = Math.max(1, (listLeft + contentW - rightX) / (ICON + 2));
        int iconRows = (e.relatedItems().size() + perRow - 1) / perRow;
        int rightH = ROW_PAD + (e.relatedHeader() != null ? LINE_H : 0) + iconRows * (ICON + 2) + ROW_PAD;
        return Math.max(leftH, rightH);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
