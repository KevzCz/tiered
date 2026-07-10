package draylar.tiered.mixin.client;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class CursedItemMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void injectCursedTooltip(
            Item.TooltipContext context,
            @Nullable Player player,
            TooltipFlag type,
            CallbackInfoReturnable<List<Component>> cir
    ) {
        ItemStack stack = (ItemStack) (Object) this;

        ResourceLocation attributeId = ModifierUtils.getAttributeId(stack);
        if (attributeId == null) return;

        PotentialAttribute attribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(attributeId);
        if (attribute == null || !attribute.isCursed()) return;

        List<Component> tooltip = cir.getReturnValue();

        String displayName = formatModifierName(attributeId.getPath());
        MutableComponent cursedText = Component.translatable("tooltip.tiered.cursed", displayName)
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);

        Font renderer = Minecraft.getInstance().font;
        int maxWidth = tooltip.stream().mapToInt(renderer::width).max().orElse(renderer.width(cursedText));
        int cursedWidth = renderer.width(cursedText);
        int spaceWidth = renderer.width(" ");
        int spaceCount = (maxWidth - cursedWidth) / 2 / spaceWidth;
        String padding = " ".repeat(Math.max(0, spaceCount));
        MutableComponent centered = Component.literal(padding).append(cursedText);

        tooltip.removeIf(line -> line.getString().equalsIgnoreCase(cursedText.getString()));

        int insertIndex = -1;
        for (int i = 0; i < tooltip.size(); i++) {
            if (!tooltip.get(i).getString().trim().isEmpty()) {
                insertIndex = i + 1;
                break;
            }
        }

        if (insertIndex == -1 || insertIndex > tooltip.size()) {
            tooltip.add(centered);
        } else {
            tooltip.add(insertIndex, centered);
        }
    }

    private String formatModifierName(String rawId) {
        String[] parts = rawId.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return builder.toString().trim();
    }
}
