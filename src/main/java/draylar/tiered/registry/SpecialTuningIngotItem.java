package draylar.tiered.registry;

import draylar.tiered.api.SpecialStatsComponent;
import draylar.tiered.config.BasicStatEntry;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.SpecialIngotConfig;
import draylar.tiered.config.SpecialStatEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpecialTuningIngotItem extends Item {
    public SpecialTuningIngotItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        SpecialStatsComponent comp = stack.get(ModComponents.SPECIAL_STATS);
        if (comp == null) {
            tooltip.add(Text.literal("Unidentified").formatted(Formatting.GRAY, Formatting.ITALIC));
        } else {
            tooltip.add(Text.literal("Special").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
            for (SpecialStatsComponent.Entry e : comp.specials()) {
                tooltip.add(Text.literal(statLine(e)).formatted(Formatting.LIGHT_PURPLE));
            }
            tooltip.add(Text.literal(statLine(comp.basic())).formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient) {
            ensureRoll(stack);
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public static void ensureRoll(ItemStack stack) {
        if (stack.get(ModComponents.SPECIAL_STATS) != null) return;

        SpecialIngotConfig cfg = ConfigInit.SPECIAL_INGOT;
        if (cfg == null) return;

        List<SpecialStatEntry> pool = cfg.specialStats;
        List<BasicStatEntry> basics = cfg.basicStats;
        if (pool == null || pool.size() < 3 || basics == null || basics.isEmpty()) return;

        Random r = new Random();

        // choose 3 distinct special stats by weight
        List<SpecialStatEntry> chosen = pickWeighted(pool, 3, r);

        // choose 1 basic stat by weight
        BasicStatEntry basic = (BasicStatEntry) pickWeighted(basics, 1, r).get(0);

        // split total special percentage randomly across the 3 chosen specials
        double total = cfg.totalSpecialPercent;
        double a = r.nextDouble(), b = r.nextDouble(), c = r.nextDouble();
        double sum = a + b + c;
        double va = total * a / sum, vb = total * b / sum, vc = total * c / sum;

        List<SpecialStatsComponent.Entry> specials = new ArrayList<>();
        specials.add(toEntry(chosen.get(0), va));
        specials.add(toEntry(chosen.get(1), vb));
        specials.add(toEntry(chosen.get(2), vc));

        SpecialStatsComponent.Entry basicEntry =
                new SpecialStatsComponent.Entry(basic.attributeId, basic.value, basic.operation, basic.slots);

        stack.set(ModComponents.SPECIAL_STATS, new SpecialStatsComponent(specials, basicEntry));
    }

    private static SpecialStatsComponent.Entry toEntry(SpecialStatEntry s, double value) {
        return new SpecialStatsComponent.Entry(s.attributeId, value, s.operation, s.slots);
    }

    private static <T> List<T> pickWeighted(List<? extends Object> list, int count, Random r) {
        List<T> out = new ArrayList<>();
        List<Integer> used = new ArrayList<>();
        while (out.size() < count && used.size() < list.size()) {
            double total = 0.0;
            for (int i = 0; i < list.size(); i++) {
                if (used.contains(i)) continue;
                double w = weightOf(list.get(i));
                if (w > 0) total += w;
            }
            if (total <= 0) break;

            double roll = r.nextDouble() * total;
            for (int i = 0; i < list.size(); i++) {
                if (used.contains(i)) continue;
                double w = weightOf(list.get(i));
                if (w <= 0) continue;
                if (roll < w) {
                    used.add(i);
                    @SuppressWarnings("unchecked")
                    T picked = (T) list.get(i);
                    out.add(picked);
                    break;
                }
                roll -= w;
            }
        }
        return out;
    }

    private static double weightOf(Object o) {
        if (o instanceof SpecialStatEntry s) return s.weight;
        if (o instanceof BasicStatEntry b) return b.weight;
        return 1.0;
    }

    private static String statLine(SpecialStatsComponent.Entry e) {
        String amount = e.operation() == 2
                ? String.format("+%.1f%% ", e.value() * 100.0)
                : String.format("%+,.2f ", e.value());
        String attr = Identifier.of(e.attribute())
                .getPath()
                .replace("generic.", "")
                .replace('_', ' ');
        return amount + attr;
    }
}
