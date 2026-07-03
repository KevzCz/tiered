package draylar.tiered.api.imprint;

import java.util.List;

import draylar.tiered.api.effect.ReforgeEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
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

    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
    }

    public void onRemove(ItemStack stack) {
    }

    public boolean isEligible(ItemStack stack) {
        return isEligible(stack, null);
    }

    public boolean isEligible(ItemStack stack, @Nullable PlayerEntity player) {
        return true;
    }

    public void appendTooltip(ItemStack stack, ImprintComponent.Entry entry, List<Text> tooltip) {
        tooltip.add(Text.translatable(translationKey()).styled(s -> s.withColor(color())));
    }

    public abstract String translationKey();

    public String nameKey() {
        return translationKey() + ".name";
    }

    public Formatting color() {
        return Formatting.GRAY;
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
