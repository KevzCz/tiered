package draylar.tiered.api;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.Text;

public record ImprintPlatesData(Text header, List<SlotView> slots, int capacity, List<Text> siblingLines)
        implements TooltipData {

    public record SlotView(ItemStack rune, List<Plate> plates) {
    }

    public record Plate(Text label, int fillColor, float cooldownFill, boolean active, String imprintId, String abilityId) {

        public Plate(Text label, int fillColor) {
            this(label, fillColor, 1.0f, false, "", "");
        }

        public Plate(Text label, int fillColor, float cooldownFill, boolean active) {
            this(label, fillColor, cooldownFill, active, "", "");
        }

        public Plate(Text label, int fillColor, float cooldownFill, boolean active, String imprintId) {
            this(label, fillColor, cooldownFill, active, imprintId, "");
        }
    }
}
