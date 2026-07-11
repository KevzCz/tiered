package draylar.tiered.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import draylar.tiered.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    protected AbstractContainerMenu menu;

    public HandledScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true)
    protected void drawMouseoverTooltipMixin(GuiGraphics context, int x, int y, CallbackInfo info) {
        if (!this.menu.getCarried().isEmpty() || this.hoveredSlot == null || !this.hoveredSlot.hasItem()) {
            return;
        }
        ItemStack stack = this.hoveredSlot.getItem();
        if (TieredTooltip.tryRenderTieredBorder(context, this.font, stack, x, y, DefaultTooltipPositioner.INSTANCE)) {
            info.cancel();
        }
    }

}
