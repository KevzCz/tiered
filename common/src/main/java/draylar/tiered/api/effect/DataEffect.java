package draylar.tiered.api.effect;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.imprint.Imprints;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.registry.ModComponents;
import draylar.tiered.util.ImprintSlots;
import draylar.tiered.util.ReforgeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DataEffect extends ReforgeEffect {

    private final String id;
    private final EffectDefinition def;
    @Nullable
    private final ReforgeEffect delegate;

    public DataEffect(String id, EffectDefinition def) {
        this.id = id;
        this.def = def;
        this.delegate = "custom".equals(def.getType()) ? ReforgeEffectRegistry.getBuiltin(def.getEffect()) : null;
    }

    public EffectDefinition definition() {
        return def;
    }

    @Override
    public boolean skipsReforge() {
        if (delegate != null) return delegate.skipsReforge();
        return def.isSkipsReforge() || "forget".equals(def.getType()) || def.isExtract();
    }

    @Override
    public boolean canRun(ItemStack target) {
        if (delegate != null) return delegate.canRun(target);

        if (!def.appliesTo(target)) return false;
        return switch (def.getType()) {
            case "forget" -> !Imprints.ids(target).isEmpty() || ModifierUtils.getAttributeId(target) != null;
            case "repair" -> target.isDamageableItem() && target.isDamaged();
            case "extract" -> Imprints.slotsUsed(target) > 0;
            case "grant_rune_slot" -> runeSlotHeadroom(target) > 0;
            default -> true;
        };
    }

    private int runeSlotHeadroom(ItemStack target) {
        int globalHeadroom = ImprintSlots.bonusHeadroom(target);
        int maxTotal = (int) def.getParam("max_total", 0f);
        if (maxTotal > 0) {
            Integer added = target.get(ModComponents.RUNE_SLOT_GRANTS);
            int alreadyAdded = added == null ? 0 : added;
            return Math.max(0, Math.min(globalHeadroom, maxTotal - alreadyAdded));
        }
        return globalHeadroom;
    }

    @Override
    public void preReforge(Player player, ItemStack stack, EffectContext context, RollBias rollBias) {
        if (delegate != null) { delegate.preReforge(player, stack, context, rollBias); return; }
        switch (def.getType()) {
            case "stabilize" -> {
                ResourceLocation current = ModifierUtils.getAttributeId(stack);
                if (current != null) {
                    String prefix = current.getPath().split("_")[0].toLowerCase();
                    int currentOrder = ReforgeUtil.getRarityOrderExact(prefix);
                    int existingOrder = rollBias.guaranteedMinRarity == null ? -1
                            : ReforgeUtil.getRarityOrderExact(rollBias.guaranteedMinRarity);
                    if (currentOrder >= 0 && currentOrder > existingOrder) {
                        rollBias.guaranteedMinRarity = prefix;
                    }
                }
            }
            case "overcharge" -> {
                int extraCost = Math.max(1, (int) context.param("extra_cost", def.getParam("extra_cost", 1f)));
                float boostPerCost = def.getParam("boost_per_cost", 0.35f);
                float baseBoost = def.rollValue(new Random());
                String minRarity = def.getParam("min_rarity_order", 2f) >= 0
                        ? rarityByOrder((int) def.getParam("min_rarity_order", 2f)) : "rare";

                rollBias.extraBaseCost += extraCost;
                rollBias.extraRarityBoost += baseBoost + boostPerCost * extraCost;

                int minOrder = ReforgeUtil.getRarityOrderExact(minRarity);
                int existingOrder = rollBias.guaranteedMinRarity == null ? -1
                        : ReforgeUtil.getRarityOrderExact(rollBias.guaranteedMinRarity);
                if (minOrder >= 0 && minOrder > existingOrder) {
                    rollBias.guaranteedMinRarity = minRarity;
                }
            }
        }
    }

    @Override
    public boolean onReforge(Player player, ItemStack stack, EffectContext context) {
        if (delegate != null) return delegate.onReforge(player, stack, context);
        String sourceId = additionId(context);
        return switch (def.getType()) {
            case "repair" -> doRepair(stack);
            case "grant_imprint" -> doGrantImprint(stack, sourceId);
            case "forget" -> doForget(stack);
            case "grant_rune_slot" -> doGrantRuneSlot(stack);
            default -> false;
        };
    }

    private boolean doGrantRuneSlot(ItemStack stack) {
        int amount = Math.max(1, Math.round(def.rollValue(new Random())));
        amount = Math.min(amount, runeSlotHeadroom(stack));
        if (amount <= 0) return false;
        int added = ImprintSlots.addBonusSlots(stack, amount);
        if (added <= 0) return false;
        Integer prior = stack.get(ModComponents.RUNE_SLOT_GRANTS);
        stack.set(ModComponents.RUNE_SLOT_GRANTS, (prior == null ? 0 : prior) + added);
        return true;
    }

    private String additionId(EffectContext context) {
        ItemStack addition = context.addition();
        if (addition != null && !addition.isEmpty()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(addition.getItem());
            if (itemId != null) return itemId.toString();
        }
        return id;
    }

    private static String rarityByOrder(int order) {
        List<String> rarities = ConfigInit.RARITY_ORDER;
        if (rarities == null || rarities.isEmpty()) {
            rarities = List.of("common", "uncommon", "rare", "epic", "legendary", "unique");
        }
        if (order < 0 || order >= rarities.size()) return "rare";
        return rarities.get(order);
    }

    private boolean doRepair(ItemStack stack) {
        if (!stack.isDamageableItem()) return false;
        int damage = stack.getDamageValue();
        if (damage <= 0) return false;
        float percent = Math.max(0f, Math.min(1f, def.rollValue(new Random())));
        int repaired = Math.round(stack.getMaxDamage() * percent);
        if (repaired <= 0) return false;
        stack.setDamageValue(Math.max(0, damage - repaired));
        return true;
    }

    private boolean doGrantImprint(ItemStack stack, String sourceId) {
        if (def.getImprint() == null) return false;
        return Imprints.grantSingle(stack, sourceId, def.getImprint(), def.rollValue(new Random()));
    }

    private boolean doForget(ItemStack stack) {
        boolean changed = Imprints.forgetAll(stack) > 0;
        if (ModifierUtils.getAttributeId(stack) != null) {
            ModifierUtils.removeItemStackAttribute(stack);
            changed = true;
        }
        return changed;
    }
}
