package draylar.tiered.reforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.network.TieredClientPacket;
import draylar.tiered.network.packet.ExtractSlotPacket;
import draylar.tiered.registry.ModComponents;
import draylar.tiered.util.ImprintPlatesComponent;
import draylar.tiered.util.ReforgeMaterialTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ExtractChooserScreen extends Screen {

    private static final int ROW_H = 22;
    private static final int ROW_GAP = 3;
    private static final int PANEL_PAD = 10;
    private static final int ICON = 16;

    private final Screen parent;
    private final ItemStack target;
    private final List<ImprintComponent.Slot> slots;
    private final int chancePercent;

    private int panelX, panelY, panelW, panelH, rowsTop;

    public ExtractChooserScreen(Screen parent, ItemStack target, int chancePercent) {
        super(Text.translatable("screen.tiered.extract.title"));
        this.parent = parent;
        this.target = target;
        this.chancePercent = chancePercent;
        ImprintComponent comp = target.get(ModComponents.IMPRINTS);
        this.slots = comp == null ? List.of() : comp.slots();
    }

    @Override
    protected void init() {
        int rows = Math.max(1, slots.size());
        panelW = 230;
        int header = 30;
        int footer = 26;
        panelH = header + rows * (ROW_H + ROW_GAP) + footer;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        rowsTop = panelY + header;

        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("gui.cancel"), b -> close())
                .dimensions(panelX + panelW / 2 - 50, panelY + panelH - footer + 3, 100, 18)
                .build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < slots.size(); i++) {
                int ry = rowsTop + i * (ROW_H + ROW_GAP);
                boolean over = mouseX >= panelX + PANEL_PAD && mouseX <= panelX + panelW - PANEL_PAD
                        && mouseY >= ry && mouseY <= ry + ROW_H;
                if (over) {
                    if (this.client != null) {
                        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f));
                    }
                    TieredClientPacket.writeC2SExtractSlotPacket(i);
                    close();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0100010);
        context.drawBorder(panelX, panelY, panelW, panelH, 0xFF6A4A8A);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelY + 8, 0xFFE0C8FF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.tiered.extract.chance", chancePercent).styled(s -> s.withColor(Formatting.GRAY)),
                this.width / 2, panelY + 19, 0xFFAAAAAA);

        if (slots.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("screen.tiered.extract.empty").styled(s -> s.withColor(Formatting.GRAY)),
                    this.width / 2, rowsTop + 6, 0xFFAAAAAA);
        }

        for (int i = 0; i < slots.size(); i++) {
            ImprintComponent.Slot slot = slots.get(i);
            int ry = rowsTop + i * (ROW_H + ROW_GAP);
            int rx = panelX + PANEL_PAD;
            int rw = panelW - PANEL_PAD * 2;
            boolean hovered = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + ROW_H;

            context.fill(rx, ry, rx + rw, ry + ROW_H, hovered ? 0xAA3A2A55 : 0x55241830);
            context.drawBorder(rx, ry, rw, ROW_H, hovered ? 0xFFB088E0 : 0xFF5A3A7A);

            int iconY = ry + (ROW_H - ICON) / 2;
            ItemStack runeIcon = runeStack(slot.sourceId());
            context.drawItem(runeIcon, rx + 3, iconY);

            int px = rx + 3 + ICON + 4;
            int py = ry + (ROW_H - ImprintPlatesComponent.plateHeight()) / 2;
            for (ImprintComponent.Entry entry : slot.entries()) {
                Imprint imprint = ImprintRegistry.get(entry.id());
                if (imprint == null) continue;
                String label = Text.translatable(imprint.nameKey()).getString().toUpperCase(Locale.ROOT);
                px += ImprintPlatesComponent.drawStandalonePlate(context, this.textRenderer, px, py, Text.literal(label), formattingToFill(imprint)) + 3;
            }
        }

    }

    private static ItemStack runeStack(String sourceId) {
        if (sourceId == null || sourceId.isEmpty()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(sourceId);
        if (id == null) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static int formattingToFill(Imprint imprint) {
        return ReforgeMaterialTooltip.imprintToFill(imprint);
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
