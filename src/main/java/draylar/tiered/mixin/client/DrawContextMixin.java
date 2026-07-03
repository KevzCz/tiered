package draylar.tiered.mixin.client;

import java.util.List;
import java.util.Optional;

import draylar.tiered.api.imprint.ImprintAttributes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import draylar.tiered.Tiered;
import draylar.tiered.TieredClient;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.util.ReforgeMaterialTooltip;
import draylar.tiered.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Shadow
    @Mutable
    @Final
    private MinecraftClient client;

    @Invoker("drawTooltip")
    abstract void tiered$drawComponentTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner);

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"), cancellable = true)
    private void tiered$swapImprintPlates(TextRenderer textRenderer, List<Text> text, Optional<TooltipData> data, int x, int y, CallbackInfo info) {
        boolean hasMarker = false;
        for (Text line : text) {
            if (ReforgeMaterialTooltip.isSlotsMarker(line)) {
                hasMarker = true;
                break;
            }
        }
        if (!hasMarker) return;

        ImprintAttributes.CONTEXT_PLAYER.set(client.player);
        if (client.player != null) ImprintAttributes.buildOverflowAllocation(client.player);
        List<TooltipComponent> components;
        try {
            components = TieredTooltip.buildComponents(text, data, null);
        } finally {
            ImprintAttributes.CONTEXT_PLAYER.remove();
            ImprintAttributes.OVERFLOW_ALLOCATION.remove();
        }
        tiered$drawComponentTooltip(textRenderer, components, x, y, HoveredTooltipPositioner.INSTANCE);
        info.cancel();
    }

    @Inject(method = "drawItemTooltip", at = @At("HEAD"), cancellable = true)
    private void drawItemTooltipMixin(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {
        if (ConfigInit.CONFIG.tieredTooltip && stack.get(Tiered.TIER) != null) {
            String nbtString = stack.get(Tiered.TIER).tier();
            for (int i = 0; i < TieredClient.BORDER_TEMPLATES.size(); i++) {
                if (!TieredClient.BORDER_TEMPLATES.get(i).containsStack(stack) && TieredClient.BORDER_TEMPLATES.get(i).containsDecider(nbtString)) {
                    TieredClient.BORDER_TEMPLATES.get(i).addStack(stack);
                } else if (TieredClient.BORDER_TEMPLATES.get(i).containsStack(stack)) {
                    ImprintAttributes.CONTEXT_PLAYER.set(client.player);
                    if (client.player != null) ImprintAttributes.buildOverflowAllocation(client.player);
                    try {
                        List<Text> text = Screen.getTooltipFromItem(client, stack);
                        List<TooltipComponent> list = TieredTooltip.buildComponents(text, stack.getTooltipData(), stack);
                        TieredTooltip.renderTieredTooltipFromComponents((DrawContext) (Object) this, textRenderer, list, x, y, HoveredTooltipPositioner.INSTANCE, TieredClient.BORDER_TEMPLATES.get(i));
                    } finally {
                        ImprintAttributes.CONTEXT_PLAYER.remove();
                        ImprintAttributes.OVERFLOW_ALLOCATION.remove();
                    }
                    info.cancel();
                    break;
                }
            }
        }
    }

}
