package draylar.tiered.mixin.client;

import draylar.tiered.lib.InventoryTab;
import draylar.tiered.lib.Tab;
import draylar.tiered.lib.TabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

@Mixin(AbstractContainerScreen.class)
public abstract class TabRenderingMixin extends Screen {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;

    @Unique private int hoveredTabIndex = -1;

    protected TabRenderingMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void tiered_renderTabs(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!(this instanceof Tab)) return;

        List<InventoryTab> tabs = TabRegistry.getTabsForScreen(this);
        if (tabs.isEmpty()) return;

        hoveredTabIndex = -1;

        int tabY = this.topPos - InventoryTab.TAB_HEIGHT;
        int tabX = this.leftPos;

        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            int currentTabX = tabX + (i * (InventoryTab.TAB_WIDTH + InventoryTab.TAB_SPACING));

            boolean selected = tab.isSelected(this.getClass());
            boolean hovered = mouseX >= currentTabX && mouseX <= currentTabX + InventoryTab.TAB_WIDTH
                           && mouseY >= tabY && mouseY <= tabY + InventoryTab.TAB_HEIGHT;

            if (hovered) {
                hoveredTabIndex = i;
            }

            tab.render(context, currentTabX, tabY, selected, hovered);
        }

        if (hoveredTabIndex >= 0 && hoveredTabIndex < tabs.size()) {
            tabs.get(hoveredTabIndex).renderTooltip(context, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tiered_onTabClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!(this instanceof Tab)) return;
        if (button != 0) return;

        List<InventoryTab> tabs = TabRegistry.getTabsForScreen(this);
        if (tabs.isEmpty()) return;

        int tabY = this.topPos - InventoryTab.TAB_HEIGHT;
        int tabX = this.leftPos;

        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            int currentTabX = tabX + (i * (InventoryTab.TAB_WIDTH + InventoryTab.TAB_SPACING));

            if (mouseX >= currentTabX && mouseX <= currentTabX + InventoryTab.TAB_WIDTH
                && mouseY >= tabY && mouseY <= tabY + InventoryTab.TAB_HEIGHT) {

                if (!tab.isSelected(this.getClass())) {
                    tab.onClick(Minecraft.getInstance());
                }
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
