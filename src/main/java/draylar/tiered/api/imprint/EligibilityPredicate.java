package draylar.tiered.api.imprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.annotations.SerializedName;

import draylar.tiered.Tiered;
import draylar.tiered.compat.AccessoriesCompat;
import draylar.tiered.registry.ModComponents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class EligibilityPredicate {

    @Nullable
    @SerializedName("required_item_tags")
    private final List<String> requiredItemTags;

    @Nullable
    @SerializedName("forbidden_item_tags")
    private final List<String> forbiddenItemTags;

    @Nullable
    @SerializedName("required_items")
    private final List<String> requiredItems;

    @Nullable
    @SerializedName("forbidden_items")
    private final List<String> forbiddenItems;

    @Nullable
    @SerializedName("required_slots")
    private final List<String> requiredSlots;

    @Nullable
    @SerializedName("min_durability_fraction")
    private final Float minDurabilityFraction;

    @Nullable
    @SerializedName("max_durability_fraction")
    private final Float maxDurabilityFraction;

    @Nullable
    @SerializedName("required_enchantments")
    private final List<String> requiredEnchantments;

    @Nullable
    @SerializedName("forbidden_enchantments")
    private final List<String> forbiddenEnchantments;

    @Nullable
    @SerializedName("min_imprint_count")
    private final Integer minImprintCount;

    @Nullable
    @SerializedName("max_imprint_count")
    private final Integer maxImprintCount;

    @Nullable
    @SerializedName("requires_damaged")
    private final Boolean requiresDamaged;

    @Nullable
    @SerializedName("held_item_tags")
    private final List<String> heldItemTags;

    @Nullable
    @SerializedName("held_items")
    private final List<String> heldItems;

    @Nullable
    @SerializedName("equipped_item")
    private final EquippedItemClause equippedItem;

    @Nullable
    @SerializedName("accessories_equipped_tags")
    private final List<String> accessoriesEquippedTags;

    @Nullable
    @SerializedName("accessories_equipped_items")
    private final List<String> accessoriesEquippedItems;

    public static EligibilityPredicate withEquippedItem(EquippedItemClause clause) {
        return new EligibilityPredicate(null, null, null, clause);
    }

    public EligibilityPredicate(
            @Nullable List<String> requiredItemTags,
            @Nullable List<String> forbiddenItemTags,
            @Nullable List<String> requiredSlots,
            @Nullable EquippedItemClause equippedItem) {
        this.requiredItemTags = requiredItemTags;
        this.forbiddenItemTags = forbiddenItemTags;
        this.requiredSlots = requiredSlots;
        this.requiredItems = null;
        this.forbiddenItems = null;
        this.minDurabilityFraction = null;
        this.maxDurabilityFraction = null;
        this.requiredEnchantments = null;
        this.forbiddenEnchantments = null;
        this.minImprintCount = null;
        this.maxImprintCount = null;
        this.requiresDamaged = null;
        this.heldItemTags = null;
        this.heldItems = null;
        this.equippedItem = equippedItem;
        this.accessoriesEquippedTags = null;
        this.accessoriesEquippedItems = null;
    }

    public static class EquippedItemClause {
        @Nullable private String slot;
        @Nullable private List<String> tags;
        @Nullable @SerializedName("any_tags") private List<String> anyTags;
        @Nullable private List<String> items;

        public EquippedItemClause() {}

        public static EquippedItemClause ofTags(String slot, List<String> tags) {
            EquippedItemClause c = new EquippedItemClause();
            c.slot = slot; c.tags = tags; return c;
        }

        public static EquippedItemClause ofAnyTags(String slot, List<String> anyTags) {
            EquippedItemClause c = new EquippedItemClause();
            c.slot = slot; c.anyTags = anyTags; return c;
        }

        public static EquippedItemClause ofItems(String slot, List<String> items) {
            EquippedItemClause c = new EquippedItemClause();
            c.slot = slot; c.items = items; return c;
        }

        @Nullable public String getSlot() { return slot; }
        @Nullable public List<String> getTags() { return tags; }
        @Nullable public List<String> getAnyTags() { return anyTags; }
        @Nullable public List<String> getItems() { return items; }
    }

    public boolean isEligible(ItemStack stack) {
        return isEligible(stack, null);
    }

    public boolean isEligible(ItemStack stack, @Nullable PlayerEntity player) {
        if (stack == null || stack.isEmpty()) return false;

        if (requiredItemTags != null) {
            for (String tag : requiredItemTags) {
                if (!stack.isIn(tagKey(tag))) return false;
            }
        }
        if (forbiddenItemTags != null) {
            for (String tag : forbiddenItemTags) {
                if (stack.isIn(tagKey(tag))) return false;
            }
        }
        if (requiredItems != null && !requiredItems.isEmpty()) {
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            boolean anyMatch = false;
            for (String id : requiredItems) {
                if (itemId.toString().equals(id)) { anyMatch = true; break; }
            }
            if (!anyMatch) return false;
        }
        if (forbiddenItems != null) {
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            for (String id : forbiddenItems) {
                if (itemId.toString().equals(id)) return false;
            }
        }
        if (requiredSlots != null && !requiredSlots.isEmpty()) {
            boolean any = false;
            for (String slotName : requiredSlots) {
                EquipmentSlot slot = parseEquipmentSlot(slotName);
                if (slot != null && Tiered.isPreferredEquipmentSlot(stack, slot)) {
                    any = true;
                    break;
                }
            }
            if (!any) return false;
        }
        if (minDurabilityFraction != null || maxDurabilityFraction != null) {
            int maxDur = stack.getMaxDamage();
            if (maxDur > 0) {
                float fraction = 1f - (float) stack.getDamage() / maxDur;
                if (minDurabilityFraction != null && fraction < minDurabilityFraction) return false;
                if (maxDurabilityFraction != null && fraction > maxDurabilityFraction) return false;
            }
        }
        if (requiresDamaged != null) {
            boolean isDamaged = stack.isDamaged();
            if (requiresDamaged && !isDamaged) return false;
            if (!requiresDamaged && isDamaged) return false;
        }
        if (requiredEnchantments != null && !requiredEnchantments.isEmpty()) {
            boolean anyEnchant = false;
            var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants != null) {
                for (var entry : enchants.getEnchantments()) {
                    String enchId = entry.getKey().map(k -> k.getValue().toString()).orElse("");
                    for (String req : requiredEnchantments) {
                        if (enchId.equals(req)) { anyEnchant = true; break; }
                    }
                    if (anyEnchant) break;
                }
            }
            if (!anyEnchant) return false;
        }
        if (forbiddenEnchantments != null && !forbiddenEnchantments.isEmpty()) {
            var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants != null) {
                for (var entry : enchants.getEnchantments()) {
                    String enchId = entry.getKey().map(k -> k.getValue().toString()).orElse("");
                    for (String forb : forbiddenEnchantments) {
                        if (enchId.equals(forb)) return false;
                    }
                }
            }
        }
        if (minImprintCount != null || maxImprintCount != null) {
            ImprintComponent comp = stack.get(ModComponents.IMPRINTS);
            int count = comp == null ? 0 : comp.slotsUsed();
            if (minImprintCount != null && count < minImprintCount) return false;
            if (maxImprintCount != null && count > maxImprintCount) return false;
        }

        if (heldItemTags != null && !heldItemTags.isEmpty()) {
            if (player == null) return false;
            if (!stackMatchesAllTags(player.getMainHandStack(), heldItemTags)
                    && !stackMatchesAllTags(player.getOffHandStack(), heldItemTags)) return false;
        }
        if (heldItems != null && !heldItems.isEmpty()) {
            if (player == null) return false;
            if (!stackMatchesAnyItem(player.getMainHandStack(), heldItems)
                    && !stackMatchesAnyItem(player.getOffHandStack(), heldItems)) return false;
        }
        if (equippedItem != null) {
            if (player == null) return false;
            if (!checkEquippedItem(player, equippedItem)) return false;
        }
        if (accessoriesEquippedTags != null && !accessoriesEquippedTags.isEmpty()) {
            if (player == null) return false;
            if (!FabricLoader.getInstance().isModLoaded("accessories")) return false;
            boolean found = false;
            for (ItemStack acc : AccessoriesCompat.getEquippedAccessoryStacks(player)) {
                if (acc == null || acc.isEmpty()) continue;
                if (stackMatchesAllTags(acc, accessoriesEquippedTags)) { found = true; break; }
            }
            if (!found) return false;
        }
        if (accessoriesEquippedItems != null && !accessoriesEquippedItems.isEmpty()) {
            if (player == null) return false;
            if (!FabricLoader.getInstance().isModLoaded("accessories")) return false;
            boolean found = false;
            for (ItemStack acc : AccessoriesCompat.getEquippedAccessoryStacks(player)) {
                if (acc != null && !acc.isEmpty() && stackMatchesAnyItem(acc, accessoriesEquippedItems)) {
                    found = true;
                    break;
                }
            }
            return found;
        }

        return true;
    }

    private static boolean checkEquippedItem(PlayerEntity player, EquippedItemClause clause) {
        String slotName = clause.getSlot();
        List<String> tags = clause.getTags();
        List<String> anyTags = clause.getAnyTags();
        List<String> items = clause.getItems();
        if (slotName == null) return true;

        List<ItemStack> candidates = new ArrayList<>();
        switch (slotName.toLowerCase()) {
            case "held" -> {
                candidates.add(player.getMainHandStack());
                candidates.add(player.getOffHandStack());
            }
            case "any_armor" -> {
                candidates.add(player.getEquippedStack(EquipmentSlot.HEAD));
                candidates.add(player.getEquippedStack(EquipmentSlot.CHEST));
                candidates.add(player.getEquippedStack(EquipmentSlot.LEGS));
                candidates.add(player.getEquippedStack(EquipmentSlot.FEET));
            }
            default -> {
                EquipmentSlot slot = parseEquipmentSlot(slotName);
                if (slot != null) {
                    candidates.add(player.getEquippedStack(slot));
                } else if (FabricLoader.getInstance().isModLoaded("accessories")) {
                    candidates.addAll(AccessoriesCompat.getEquippedAccessoryStacksForSlot(player, slotName));
                }
            }
        }

        for (ItemStack candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) continue;
            boolean tagOk = tags == null || tags.isEmpty() || stackMatchesAllTags(candidate, tags);
            boolean anyTagOk = anyTags == null || anyTags.isEmpty() || stackMatchesAnyTag(candidate, anyTags);
            boolean itemOk = items == null || items.isEmpty() || stackMatchesAnyItem(candidate, items);
            if (tagOk && anyTagOk && itemOk) return true;
        }
        return false;
    }

    private static boolean stackMatchesAllTags(ItemStack stack, List<String> tags) {
        if (stack == null || stack.isEmpty()) return false;
        for (String tag : tags) {
            if (!stack.isIn(tagKey(tag))) return false;
        }
        return true;
    }

    private static boolean stackMatchesAnyTag(ItemStack stack, List<String> tags) {
        if (stack == null || stack.isEmpty()) return false;
        for (String tag : tags) {
            if (stack.isIn(tagKey(tag))) return true;
        }
        return false;
    }

    private static boolean stackMatchesAnyItem(ItemStack stack, List<String> items) {
        if (stack == null || stack.isEmpty()) return false;
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        String idStr = itemId.toString();
        for (String id : items) {
            if (idStr.equals(id)) return true;
        }
        return false;
    }

    private static final Map<String, TagKey<Item>> TAG_KEY_CACHE = new ConcurrentHashMap<>();

    private static TagKey<Item> tagKey(String tag) {
        return TAG_KEY_CACHE.computeIfAbsent(tag, t -> {
            String path = t.startsWith("#") ? t.substring(1) : t;
            return TagKey.of(RegistryKeys.ITEM, Identifier.of(path));
        });
    }

    @Nullable
    private static EquipmentSlot parseEquipmentSlot(String name) {
        if (name == null) return null;
        return switch (name.toLowerCase()) {
            case "mainhand", "main_hand" -> EquipmentSlot.MAINHAND;
            case "offhand", "off_hand" -> EquipmentSlot.OFFHAND;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    public boolean hasItemRequirement() {
        return notEmpty(requiredItemTags) || notEmpty(requiredItems)
                || notEmpty(heldItemTags) || notEmpty(heldItems)
                || notEmpty(accessoriesEquippedTags) || notEmpty(accessoriesEquippedItems)
                || (equippedItem != null && (notEmpty(equippedItem.getTags())
                        || notEmpty(equippedItem.getAnyTags()) || notEmpty(equippedItem.getItems())));
    }

    public List<ItemStack> requirementIcons() {
        List<ItemStack> out = new ArrayList<>();
        addTagIcons(out, requiredItemTags);
        addTagIcons(out, heldItemTags);
        addTagIcons(out, accessoriesEquippedTags);
        addItemIcons(out, requiredItems);
        addItemIcons(out, heldItems);
        addItemIcons(out, accessoriesEquippedItems);
        if (equippedItem != null) {
            addTagIcons(out, equippedItem.getTags());
            addTagIcons(out, equippedItem.getAnyTags());
            addItemIcons(out, equippedItem.getItems());
        }
        return out;
    }

    public List<String> requirementTags() {
        List<String> out = new ArrayList<>();
        addAll(out, requiredItemTags);
        addAll(out, heldItemTags);
        addAll(out, accessoriesEquippedTags);
        addAll(out, requiredItems);
        addAll(out, heldItems);
        addAll(out, accessoriesEquippedItems);
        if (equippedItem != null) {
            addAll(out, equippedItem.getTags());
            addAll(out, equippedItem.getAnyTags());
            addAll(out, equippedItem.getItems());
        }
        return out;
    }

    private static void addAll(List<String> out, @Nullable List<String> src) {
        if (src == null) return;
        for (String s : src) if (s != null && !out.contains(s)) out.add(s);
    }

    private static void addTagIcons(List<ItemStack> out, @Nullable List<String> tags) {
        if (tags == null) return;
        for (String tag : tags) {
            for (var entry : Registries.ITEM.iterateEntries(tagKey(tag))) {
                ItemStack s = new ItemStack(entry.value());
                if (!s.isEmpty()) out.add(s);
            }
        }
    }

    private static void addItemIcons(List<ItemStack> out, @Nullable List<String> items) {
        if (items == null) return;
        for (String id : items) {
            Item item = Registries.ITEM.get(Identifier.of(id));
            ItemStack s = new ItemStack(item);
            if (!s.isEmpty()) out.add(s);
        }
    }

    private static boolean notEmpty(@Nullable List<String> l) {
        return l != null && !l.isEmpty();
    }
}
