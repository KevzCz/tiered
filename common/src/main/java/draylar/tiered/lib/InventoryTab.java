package draylar.tiered.lib;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class InventoryTab {

    protected final Component title;
    @Nullable
    protected final ResourceLocation texture;
    protected final int preferredPos;
    protected final Class<?>[] screenClasses;

    public static final int TAB_WIDTH = 28;
    public static final int TAB_HEIGHT = 28;
    public static final int TAB_SPACING = 1;

    public InventoryTab(Component title, @Nullable ResourceLocation texture, int preferredPos, Class<?>... screenClasses) {
        this.title = title;
        this.texture = texture;
        this.preferredPos = preferredPos;
        this.screenClasses = screenClasses;
    }

    public abstract void onClick(Minecraft client);

    public void render(GuiGraphics context, int x, int y, boolean selected, boolean hovered) {
        int bgColor = selected ? 0xFFC6C6C6 : (hovered ? 0xFFB0B0B0 : 0xFF8B8B8B);
        int borderColor = 0xFF373737;

        int drawY = selected ? y : y + 3;
        int drawHeight = selected ? TAB_HEIGHT : TAB_HEIGHT - 3;

        context.fill(x, drawY, x + TAB_WIDTH, drawY + drawHeight, bgColor);

        context.fill(x, drawY, x + TAB_WIDTH, drawY + 1, borderColor);
        context.fill(x, drawY, x + 1, drawY + drawHeight, borderColor);
        context.fill(x + TAB_WIDTH - 1, drawY, x + TAB_WIDTH, drawY + drawHeight, borderColor);
        if (!selected) {
            context.fill(x, drawY + drawHeight - 1, x + TAB_WIDTH, drawY + drawHeight, borderColor);
        }

        if (texture != null) {
            int iconY = drawY + (drawHeight - 16) / 2;
            context.blit(texture, x + 6, iconY, 0, 0, 16, 16, 16, 16);
        }
    }

    public void renderTooltip(GuiGraphics context, int mouseX, int mouseY) {
        context.renderTooltip(Minecraft.getInstance().font, title, mouseX, mouseY);
    }

    public int getPreferredPos() {
        return preferredPos;
    }

    public boolean isSelected(Class<?> screenClass) {
        for (Class<?> clazz : screenClasses) {
            if (clazz.equals(screenClass)) {
                return true;
            }
        }
        return false;
    }
}
