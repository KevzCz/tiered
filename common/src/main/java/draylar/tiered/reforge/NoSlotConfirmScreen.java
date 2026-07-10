package draylar.tiered.reforge;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import java.util.List;

@Environment(EnvType.CLIENT)
public class NoSlotConfirmScreen extends Screen {

    private static final int PANEL_FILL      = 0xF0140A1C;
    private static final int PANEL_FILL_TOP  = 0xF01E1230;
    private static final int BORDER          = 0xFF6A4A8A;
    private static final int BORDER_GLOW     = 0xFFB088E0;
    private static final int ACCENT          = 0xFFE0C8FF;
    private static final int WARN            = 0xFFFFC94A;
    private static final int WARN_DIM        = 0x66FFC94A;
    private static final int BODY            = 0xFFC8B8D8;

    private static final int PANEL_W = 252;
    private static final int BTN_W   = 220;
    private static final int BTN_H   = 20;
    private static final int BTN_GAP = 6;

    private final Screen parent;
    private final Runnable onConfirm;
    private final Runnable onConfirmDontAsk;

    private final Btn[] buttons = new Btn[3];
    private List<FormattedCharSequence> bodyLines = List.of();
    private int panelX, panelY, panelW, panelH;
    private float time;

    public NoSlotConfirmScreen(Screen parent, Runnable onConfirm, Runnable onConfirmDontAsk) {
        super(Component.translatable("screen.tiered.reforge.confirm.no_slot.title"));
        this.parent = parent;
        this.onConfirm = onConfirm;
        this.onConfirmDontAsk = onConfirmDontAsk;
    }

    @Override
    protected void init() {
        bodyLines = this.font.split(
                Component.translatable("screen.tiered.reforge.confirm.no_slot.body"), PANEL_W - 32);

        int header = 48;
        int bodyH  = bodyLines.size() * (this.font.lineHeight + 2) + 10;
        int btnsH  = buttons.length * BTN_H + (buttons.length - 1) * BTN_GAP;
        int footer = 12;

        panelW = PANEL_W;
        panelH = header + bodyH + 8 + btnsH + footer;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int bx = panelX + (panelW - BTN_W) / 2;
        int by = panelY + header + bodyH + 8;

        buttons[0] = new Btn(bx, by, Component.translatable("screen.tiered.reforge.confirm.no_slot.yes"),
                BtnStyle.PRIMARY, () -> { onConfirm.run(); back(); });
        buttons[1] = new Btn(bx, by + BTN_H + BTN_GAP, Component.translatable("screen.tiered.reforge.confirm.no_slot.yes_dont_ask"),
                BtnStyle.SECONDARY, () -> { onConfirmDontAsk.run(); back(); });
        buttons[2] = new Btn(bx, by + 2 * (BTN_H + BTN_GAP), Component.translatable("gui.cancel"),
                BtnStyle.CANCEL, this::back);
    }

    private void back() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Btn b : buttons) {
                if (b != null && b.contains(mouseX, mouseY)) {
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
                    }
                    b.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        time += delta;

        context.fill(0, 0, this.width, this.height, 0x88000000);

        float pulse = (Mth.sin(time * 0.15f) + 1f) * 0.5f;
        int glow = blend(BORDER, BORDER_GLOW, pulse);

        context.renderOutline(panelX - 2, panelY - 2, panelW + 4, panelH + 4, withAlpha(glow, 0x44));

        context.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_FILL_TOP, PANEL_FILL);
        context.renderOutline(panelX, panelY, panelW, panelH, glow);

        int cx = this.width / 2;

        int crestY = panelY + 12;
        drawWarningCrest(context, cx, crestY + 6, pulse);

        int titleY = crestY + 18;
        context.drawCenteredString(this.font, this.title, cx, titleY, ACCENT);

        int divY = titleY + 12;
        context.fill(panelX + 18, divY, panelX + panelW - 18, divY + 1, withAlpha(glow, 0x99));

        int by = divY + 8;
        for (FormattedCharSequence line : bodyLines) {
            int lw = this.font.width(line);
            context.drawString(this.font, line, cx - lw / 2, by, BODY);
            by += this.font.lineHeight + 2;
        }

        for (Btn b : buttons) {
            if (b != null) b.render(context, mouseX, mouseY);
        }
    }

    private void drawWarningCrest(GuiGraphics context, int cx, int cy, float pulse) {
        int r = 9;
        int ring = blend(WARN_DIM, WARN, pulse);

        for (int i = -r; i <= r; i++) {
            int span = r - Math.abs(i);
            context.fill(cx - span, cy + i, cx - span + 1, cy + i + 1, ring);
            context.fill(cx + span, cy + i, cx + span + 1, cy + i + 1, ring);
        }

        context.fill(cx - r + 3, cy - r + 3, cx + r - 2, cy + r - 2, 0x33FFC94A);

        context.fill(cx, cy - 5, cx + 1, cy + 2, WARN);
        context.fill(cx, cy + 4, cx + 1, cy + 5, WARN);
    }

    private static int blend(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (aa << 24) | (rr << 16) | (rg << 8) | rb;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    @Override
    public void onClose() {
        back();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum BtnStyle {
        PRIMARY  (0xCC4A2E6E, 0xFF7A4ABE, 0xFFEFE2FF, 0xFF8A5AD0),
        SECONDARY(0x88241830, 0xFF5A3A7A, 0xFFD8C8E8, 0xFF7A5A9A),
        CANCEL   (0x66201018, 0xFF6A3A4A, 0xFFD8B0B8, 0xFF9A5A6A);

        final int fill, border, text, borderHover;
        BtnStyle(int fill, int border, int text, int borderHover) {
            this.fill = fill; this.border = border; this.text = text; this.borderHover = borderHover;
        }
    }

    private final class Btn {
        final int x, y;
        final Component label;
        final BtnStyle style;
        final Runnable action;

        Btn(int x, int y, Component label, BtnStyle style, Runnable action) {
            this.x = x; this.y = y; this.label = label; this.style = style; this.action = action;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + BTN_W && my >= y && my <= y + BTN_H;
        }

        void render(GuiGraphics context, int mouseX, int mouseY) {
            boolean hovered = contains(mouseX, mouseY);
            int fill = hovered ? brighten(style.fill) : style.fill;
            context.fill(x, y, x + BTN_W, y + BTN_H, fill);
            context.renderOutline(x, y, BTN_W, BTN_H, hovered ? style.borderHover : style.border);

            if (hovered) context.fill(x + 1, y + 1, x + BTN_W - 1, y + 2, withAlpha(style.borderHover, 0x55));

            int tw = NoSlotConfirmScreen.this.font.width(label);
            int tx = x + (BTN_W - tw) / 2;
            int ty = y + (BTN_H - NoSlotConfirmScreen.this.font.lineHeight) / 2 + 1;
            context.drawString(NoSlotConfirmScreen.this.font,
                    label.copy().withStyle(s -> s.withColor(hovered ? 0xFFFFFFFF : style.text)), tx, ty, style.text);
        }

        private int brighten(int argb) {
            int a = (argb >>> 24) & 0xFF;
            int r = Math.min(255, ((argb >> 16) & 0xFF) + 30);
            int g = Math.min(255, ((argb >> 8) & 0xFF) + 30);
            int b = Math.min(255, (argb & 0xFF) + 30);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }
}
