package draylar.tiered.mixin.client;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class CursedItemMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void injectCursedTooltip(
            Item.TooltipContext context,
            @Nullable PlayerEntity player,
            TooltipType type,
            CallbackInfoReturnable<List<Text>> cir
    ) {
        ItemStack stack = (ItemStack) (Object) this;

        Identifier attributeId = ModifierUtils.getAttributeId(stack);
        if (attributeId == null) return;

        PotentialAttribute attribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(attributeId);
        if (attribute == null || !attribute.isCursed()) return;

        List<Text> tooltip = cir.getReturnValue();

        String displayName = formatModifierName(attributeId.getPath());
        MutableText cursedText = Text.translatable("tooltip.tiered.cursed", displayName)
                .formatted(Formatting.DARK_RED, Formatting.BOLD);

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        int maxWidth = tooltip.stream().mapToInt(renderer::getWidth).max().orElse(renderer.getWidth(cursedText));
        int cursedWidth = renderer.getWidth(cursedText);
        int spaceWidth = renderer.getWidth(" ");
        int spaceCount = (maxWidth - cursedWidth) / 2 / spaceWidth;
        String padding = " ".repeat(Math.max(0, spaceCount));
        MutableText centered = Text.literal(padding).append(cursedText);

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
