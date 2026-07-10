package draylar.tiered.api.imprint;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import draylar.tiered.api.effect.ReforgeEffect;
import org.jetbrains.annotations.Nullable;

public abstract class Imprint {

    public enum Multiplicity {
        SLOT,
        MERGE
    }

    public enum ReapplyMode {
        REPLACE,
        ACCUMULATE,
        HIGHEST,
        REJECT
    }

    public enum CombineMode {
        ADDITIVE,
        HIGHEST,
        UNIQUE
    }

    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
    }

    public void onRemove(ItemStack stack) {
    }

    public boolean isEligible(ItemStack stack) {
        return isEligible(stack, null);
    }

    public boolean isEligible(ItemStack stack, @Nullable Player player) {
        return true;
    }

    public void appendTooltip(ItemStack stack, ImprintComponent.Entry entry, List<Component> tooltip) {
        tooltip.add(Component.translatable(translationKey()).withStyle(s -> s.withColor(color())));
    }

    public abstract String translationKey();

    public String nameKey() {
        return translationKey() + ".name";
    }

    public ChatFormatting color() {
        return ChatFormatting.GRAY;
    }

    public Multiplicity multiplicity() {
        return Multiplicity.MERGE;
    }

    public ReapplyMode reapplyMode() {
        return ReapplyMode.REPLACE;
    }

    public CombineMode combineMode() {
        return CombineMode.ADDITIVE;
    }

    public ImprintScope scope() {
        return ImprintScope.ANY;
    }

    public ImprintAttribute attributeData(ImprintComponent.Entry entry) {
        return null;
    }

    public final boolean isAttributeImprint() {
        return attributeData(new ImprintComponent.Entry("probe", 1, 0f)) != null;
    }
}
