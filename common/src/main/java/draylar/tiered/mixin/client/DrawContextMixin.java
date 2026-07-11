package draylar.tiered.mixin.client;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import draylar.tiered.api.imprint.ImprintAttributes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import draylar.tiered.util.ReforgeMaterialTooltip;
import draylar.tiered.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
@Mixin(GuiGraphics.class)
public abstract class DrawContextMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    private static final Method DRAW_TOOLTIP_COMPONENTS = Arrays.stream(GuiGraphics.class.getDeclaredMethods())
            .filter(m -> m.getParameterCount() == 5)
            .filter(m -> {
                Class<?>[] p = m.getParameterTypes();
                return p[0] == Font.class && p[1] == List.class && p[2] == int.class
                        && p[3] == int.class && p[4] == ClientTooltipPositioner.class;
            })
            .findFirst()
            .map(m -> {
                m.setAccessible(true);
                return m;
            })
            .orElseThrow(() -> new IllegalStateException("Could not find GuiGraphics#renderTooltipInternal(Font, List, int, int, ClientTooltipPositioner)"));

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"), cancellable = true)
    private void tiered$swapImprintPlates(Font textRenderer, List<Component> text, Optional<TooltipComponent> data, int x, int y, CallbackInfo info) {
        boolean hasMarker = false;
        for (Component line : text) {
            if (ReforgeMaterialTooltip.isSlotsMarker(line)) {
                hasMarker = true;
                break;
            }
        }
        if (!hasMarker) return;

        ImprintAttributes.CONTEXT_PLAYER.set(minecraft.player);
        if (minecraft.player != null) ImprintAttributes.buildOverflowAllocation(minecraft.player);
        List<ClientTooltipComponent> components;
        try {
            components = TieredTooltip.buildComponents(text, data, null);
        } finally {
            ImprintAttributes.CONTEXT_PLAYER.remove();
            ImprintAttributes.OVERFLOW_ALLOCATION.remove();
        }
        try {
            DRAW_TOOLTIP_COMPONENTS.invoke(this, textRenderer, components, x, y, DefaultTooltipPositioner.INSTANCE);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        info.cancel();
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
    private void drawItemTooltipMixin(Font textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {
        if (TieredTooltip.tryRenderTieredBorder((GuiGraphics) (Object) this, textRenderer, stack, x, y, DefaultTooltipPositioner.INSTANCE)) {
            info.cancel();
        }
    }

}
