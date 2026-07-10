package draylar.tiered.api;

import draylar.tiered.Tiered;
import draylar.tiered.api.effect.ReforgeEffect;
import draylar.tiered.compat.ATCCompat;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.compat.LevelZCompat;
import draylar.tiered.lib.SortList;
import draylar.tiered.util.ReforgeUtil;
import dev.architectury.platform.Platform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ModifierUtils {

    private static final String SPECIAL_TIER = "tiered:special";

    private static boolean hasAccessorylessSlots() {
        return !Platform.isModLoaded("accessories")
                && (Platform.isModLoaded("trinkets") || Platform.isModLoaded("curios"));
    }

    private static final String ACCESSORY_TIER_PREFIX = "tiered:accessory_";

    public static void refreshAccessoryTierAttributes(Player player) {
        var attrs = player.getAttributes();

        for (var entry : BuiltInRegistries.ATTRIBUTE.asHolderIdMap()) {
            var inst = attrs.getInstance(entry);
            if (inst == null) continue;
            inst.getModifiers().stream()
                    .filter(m -> m.id().toString().startsWith(ACCESSORY_TIER_PREFIX))
                    .toList()
                    .forEach(inst::removeModifier);
        }

        if (!hasAccessorylessSlots()) return;

        int index = 0;
        for (ItemStack stack : ATCCompat.getEquippedAccessoryStacks(player)) {
            if (stack == null || stack.isEmpty()) continue;
            int stackIndex = index++;
            ResourceLocation attributeId = getAttributeId(stack);
            if (attributeId == null) continue;
            PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(attributeId);
            if (potentialAttribute == null) continue;

            for (AttributeTemplate template : potentialAttribute.getAttributes()) {
                if (template.getOptionalAccessoriesSlots() == null) continue;

                Optional<Holder.Reference<Attribute>> attrRef =
                        BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(template.getAttributeTypeID()));
                if (attrRef.isEmpty()) continue;

                var inst = attrs.getInstance(attrRef.get());
                if (inst == null) continue;

                AttributeModifier base = template.getEntityAttributeModifier();
                ResourceLocation modId = ResourceLocation.fromNamespaceAndPath(
                        "tiered", "accessory_" + base.id().getPath() + "_" + stackIndex);
                inst.addTransientModifier(new AttributeModifier(modId, base.amount(), base.operation()));
            }
        }
    }

    public static Map<Holder<Attribute>, List<AttributeModifier>> getAccessoryOnlyModifiers(ItemStack stack) {
        Map<Holder<Attribute>, List<AttributeModifier>> result = new HashMap<>();
        ResourceLocation attributeId = getAttributeId(stack);
        if (attributeId == null) return result;
        PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(attributeId);
        if (potentialAttribute == null) return result;

        for (AttributeTemplate template : potentialAttribute.getAttributes()) {
            if (!template.isOnlyForAccessories()) continue;

            Optional<Holder.Reference<Attribute>> attrRef =
                    BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(template.getAttributeTypeID()));
            if (attrRef.isEmpty()) continue;

            result.computeIfAbsent(attrRef.get(), a -> new ArrayList<>()).add(template.getEntityAttributeModifier());
        }
        return result;
    }

    @Nullable
    public static ResourceLocation getRandomAttributeIDFor(@Nullable Player playerEntity, Item item, boolean reforge, @Nullable String group, boolean skipCursed) {
        return getRandomAttributeIDFor(playerEntity, item, reforge, group, skipCursed, false);
    }

    @Nullable
    public static ResourceLocation getRandomAttributeIDFor(@Nullable Player playerEntity, Item item, boolean reforge, boolean skipCursed,
            @Nullable ReforgeMaterial material) {
        List<ResourceLocation> potentialAttributes = new ArrayList<>();
        List<Integer> attributeWeights = new ArrayList<>();

        List<String> filterGroups = (material != null && material.hasGroupFilter()) ? material.getGroups() : null;

        collectValidAttributesForMaterial(item, reforge, skipCursed, filterGroups, material, potentialAttributes, attributeWeights);

        if (potentialAttributes.isEmpty() && filterGroups != null) {
            attributeWeights.clear();
            collectValidAttributesForMaterial(item, reforge, skipCursed, null, material, potentialAttributes, attributeWeights);
        }

        if (potentialAttributes.isEmpty()) {
            return null;
        }

        applyDynamicWeightModifiers(playerEntity, reforge, potentialAttributes, attributeWeights);

        if (material != null && material.getRarityBoost() > 0f) {
            applyRarityBoost(potentialAttributes, attributeWeights, material.getRarityBoost());
        }

        ResourceLocation result = weightedPick(potentialAttributes, attributeWeights);

        if (result != null && material != null && material.getGuaranteedMinRarity() != null) {
            int minOrder = ReforgeUtil.getRarityOrderExact(material.getGuaranteedMinRarity());
            if (minOrder >= 0 && rarityOrderOf(result) < minOrder) {
                List<ResourceLocation> qualifying = new ArrayList<>();
                List<Integer> qualifyingWeights = new ArrayList<>();
                for (int i = 0; i < potentialAttributes.size(); i++) {
                    if (rarityOrderOf(potentialAttributes.get(i)) >= minOrder) {
                        qualifying.add(potentialAttributes.get(i));
                        qualifyingWeights.add(attributeWeights.get(i));
                    }
                }
                if (!qualifying.isEmpty()) {
                    ResourceLocation upgraded = weightedPick(qualifying, qualifyingWeights);
                    if (upgraded != null) result = upgraded;
                }
            }
        }

        return result;
    }

    public static Map<ResourceLocation, Float> previewAttributeOdds(@Nullable Player playerEntity, Item item,
            boolean skipCursed, @Nullable ReforgeMaterial material) {
        List<ResourceLocation> potentialAttributes = new ArrayList<>();
        List<Integer> attributeWeights = new ArrayList<>();

        List<String> filterGroups = (material != null && material.hasGroupFilter()) ? material.getGroups() : null;

        collectValidAttributesForMaterial(item, true, skipCursed, filterGroups, material, potentialAttributes, attributeWeights);

        if (potentialAttributes.isEmpty() && filterGroups != null) {
            attributeWeights.clear();
            collectValidAttributesForMaterial(item, true, skipCursed, null, material, potentialAttributes, attributeWeights);
        }

        if (potentialAttributes.isEmpty()) {
            return Map.of();
        }

        applyDynamicWeightModifiers(playerEntity, true, potentialAttributes, attributeWeights);

        if (material != null && material.getRarityBoost() > 0f) {
            applyRarityBoost(potentialAttributes, attributeWeights, material.getRarityBoost());
        }

        long total = 0;
        for (int w : attributeWeights) total += Math.max(0, w);
        if (total <= 0) return Map.of();

        Map<ResourceLocation, Float> odds = new HashMap<>();

        Integer minOrder = null;
        if (material != null && material.getGuaranteedMinRarity() != null) {
            int order = ReforgeUtil.getRarityOrderExact(material.getGuaranteedMinRarity());
            if (order >= 0) minOrder = order;
        }

        if (minOrder == null) {
            for (int i = 0; i < potentialAttributes.size(); i++) {
                odds.merge(potentialAttributes.get(i), attributeWeights.get(i) / (float) total, Float::sum);
            }
            return odds;
        }

        long qualifyingTotal = 0;
        for (int i = 0; i < potentialAttributes.size(); i++) {
            if (rarityOrderOf(potentialAttributes.get(i)) >= minOrder) qualifyingTotal += Math.max(0, attributeWeights.get(i));
        }

        if (qualifyingTotal <= 0) {
            for (int i = 0; i < potentialAttributes.size(); i++) {
                odds.merge(potentialAttributes.get(i), attributeWeights.get(i) / (float) total, Float::sum);
            }
            return odds;
        }

        float belowFloorMass = 0f;
        for (int i = 0; i < potentialAttributes.size(); i++) {
            if (rarityOrderOf(potentialAttributes.get(i)) < minOrder) belowFloorMass += attributeWeights.get(i) / (float) total;
        }

        for (int i = 0; i < potentialAttributes.size(); i++) {
            ResourceLocation id = potentialAttributes.get(i);
            int w = attributeWeights.get(i);
            if (rarityOrderOf(id) < minOrder) {
                odds.merge(id, 0f, Float::sum);
            } else {
                float firstRoll = w / (float) total;
                float reRoll = belowFloorMass * (w / (float) qualifyingTotal);
                odds.merge(id, firstRoll + reRoll, Float::sum);
            }
        }
        return odds;
    }

    public static List<ResourceLocation> getProducibleAttributes(Item item, @Nullable ReforgeMaterial material) {
        List<ResourceLocation> attributes = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        List<String> filterGroups = (material != null && material.hasGroupFilter()) ? material.getGroups() : null;

        collectValidAttributesForMaterial(item, true, true, filterGroups, material, attributes, weights);

        if (material != null && material.getGuaranteedMinRarity() != null) {
            int minOrder = ReforgeUtil.getRarityOrderExact(material.getGuaranteedMinRarity());
            if (minOrder >= 0) {
                List<ResourceLocation> qualifying = new ArrayList<>();
                for (ResourceLocation id : attributes) {
                    if (rarityOrderOf(id) >= minOrder) qualifying.add(id);
                }

                if (!qualifying.isEmpty()) return qualifying;
            }
        }
        return attributes;
    }

    private static int rarityOrderOf(ResourceLocation id) {
        String prefix = id.getPath().split("_")[0];
        return ReforgeUtil.getRarityOrderExact(prefix);
    }

    private static void collectValidAttributesForMaterial(Item item, boolean reforge, boolean skipCursed, @Nullable List<String> filterGroups,
            @Nullable ReforgeMaterial material, List<ResourceLocation> outAttributes, List<Integer> outWeights) {
        List<String> lowerGroups = filterGroups == null ? null : filterGroups.stream().map(String::toLowerCase).toList();
        int maxRarityOrder = (material != null && material.getMaxRarity() != null)
                ? ReforgeUtil.getRarityOrderExact(material.getMaxRarity()) : -1;

        Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            ResourceLocation attrId = ResourceLocation.parse(attribute.getID());

            if (!attribute.isValid(BuiltInRegistries.ITEM.getKey(item))) return;
            if (attribute.getWeight() <= 0 && !reforge) return;
            if (skipCursed && attribute.isCursed()) return;

            String prefix = attrId.getPath().split("_")[0].toLowerCase();
            if (lowerGroups != null && !lowerGroups.contains(prefix)) return;

            if (maxRarityOrder >= 0) {
                int order = ReforgeUtil.getRarityOrderExact(prefix);
                if (order >= 0 && order > maxRarityOrder) return;
            }

            int weight = reforge ? attribute.getWeight() + 1 : attribute.getWeight();

            if (material != null && material.getGroupWeightMultipliers() != null) {
                Float mult = material.getGroupWeightMultipliers().get(prefix);
                if (mult != null && mult > 0f) {
                    weight = Math.max(1, Math.round(weight * mult));
                }
            }

            outAttributes.add(attrId);
            outWeights.add(weight);
        });
    }

    private static void applyRarityBoost(List<ResourceLocation> attributes, List<Integer> weights, float boost) {
        if (weights.isEmpty()) return;
        int maxWeight = Collections.max(weights);
        for (int i = 0; i < weights.size(); i++) {

            float ratio = (float) weights.get(i) / maxWeight;
            int reduced = (int) (weights.get(i) * (1.0f - boost * ratio));
            weights.set(i, Math.max(1, reduced));
        }
    }

    @Nullable
    private static ResourceLocation weightedPick(List<ResourceLocation> attributes, List<Integer> weights) {
        if (attributes.isEmpty()) return null;
        int totalWeight = 0;
        for (int i = 0; i < weights.size(); i++) {
            int w = weights.get(i);
            if (w <= 0) w = 1;
            weights.set(i, w);
            totalWeight += w;
        }
        int randomChoice = new Random().nextInt(totalWeight);
        SortList.concurrentSort(weights, weights, attributes);
        for (int i = 0; i < weights.size(); i++) {
            if (randomChoice < weights.get(i)) {
                return attributes.get(i);
            }
            randomChoice -= weights.get(i);
        }
        return attributes.get(new Random().nextInt(attributes.size()));
    }

    private static void applyDynamicWeightModifiers(@Nullable Player playerEntity, boolean reforge,
            List<ResourceLocation> potentialAttributes, List<Integer> attributeWeights) {
        if (reforge && attributeWeights.size() > 2) {
            SortList.concurrentSort(attributeWeights, attributeWeights, potentialAttributes);
            int maxWeight = attributeWeights.get(attributeWeights.size() - 1);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > maxWeight / 2) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i) * ConfigInit.CONFIG.reforgeModifier));
                }
            }
        }

        if (Tiered.isLevelZLoaded && playerEntity != null) {
            int smithingLevel = LevelZCompat.getSmithingLevel(playerEntity);
            int newMaxWeight = Collections.max(attributeWeights);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > newMaxWeight / 3) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i)
                            * (1.0f - ConfigInit.CONFIG.levelzReforgeModifier * smithingLevel)));
                }
            }
        }

        if (playerEntity != null) {
            int luckMaxWeight = Collections.max(attributeWeights);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > luckMaxWeight / 3) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i) * (1.0f - ConfigInit.CONFIG.luckReforgeModifier * playerEntity.getLuck())));
                }
            }
        }
    }

    @Nullable
    public static ResourceLocation getRandomAttributeIDFor(@Nullable Player playerEntity, Item item, boolean reforge, @Nullable String group, boolean skipCursed, boolean useAllowedGroupsFilter) {
        List<ResourceLocation> potentialAttributes = new ArrayList<>();
        List<Integer> attributeWeights = new ArrayList<>();

        List<String> allowedGroups = (useAllowedGroupsFilter && ConfigInit.ALLOWED_REROLL_GROUPS != null && !ConfigInit.ALLOWED_REROLL_GROUPS.isEmpty())
                ? ConfigInit.ALLOWED_REROLL_GROUPS
                : null;

        collectValidAttributes(playerEntity, item, reforge, group, skipCursed, allowedGroups, potentialAttributes, attributeWeights);

        if (potentialAttributes.isEmpty()) {
            attributeWeights.clear();
            collectValidAttributes(playerEntity, item, reforge, group, skipCursed, null, potentialAttributes, attributeWeights);
        }

        if (potentialAttributes.size() <= 0) {
            return null;
        }

        if (reforge && attributeWeights.size() > 2) {
            SortList.concurrentSort(attributeWeights, attributeWeights, potentialAttributes);
            int maxWeight = attributeWeights. get(attributeWeights.size() - 1);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > maxWeight / 2) {
                    attributeWeights. set(i, (int) (attributeWeights.get(i) * ConfigInit.CONFIG.reforgeModifier));
                }
            }
        }

        if (Tiered.isLevelZLoaded && playerEntity != null) {
            int smithingLevel = LevelZCompat.getSmithingLevel(playerEntity);
            int newMaxWeight = Collections.max(attributeWeights);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > newMaxWeight / 3) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i)
                            * (1.0f - ConfigInit.CONFIG.levelzReforgeModifier * smithingLevel)));
                }
            }
        }

        if (playerEntity != null) {
            int luckMaxWeight = Collections.max(attributeWeights);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > luckMaxWeight / 3) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i) * (1.0f - ConfigInit. CONFIG.luckReforgeModifier * playerEntity.getLuck())));
                }
            }
        }

        if (potentialAttributes.size() > 0) {
            int totalWeight = 0;
            for (int i = 0; i < attributeWeights.size(); i++) {
                int w = attributeWeights.get(i);
                if (w <= 0) w = 1;
                attributeWeights.set(i, w);
                totalWeight += w;
            }

            int randomChoice = new Random().nextInt(totalWeight);
            SortList. concurrentSort(attributeWeights, attributeWeights, potentialAttributes);

            for (int i = 0; i < attributeWeights.size(); i++) {
                if (randomChoice < attributeWeights.get(i)) {
                    return potentialAttributes.get(i);
                }
                randomChoice -= attributeWeights.get(i);
            }
            return potentialAttributes. get(new Random().nextInt(potentialAttributes.size()));
        } else {
            return null;
        }
    }

    private static void collectValidAttributes(
            Player playerEntity,
            Item item,
            boolean reforge,
            @Nullable String group,
            boolean skipCursed,
            @Nullable List<String> allowedGroups,
            List<ResourceLocation> outAttributes,
            List<Integer> outWeights
    ) {
        Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes(). forEach((id, attribute) -> {
            ResourceLocation attrId = ResourceLocation.parse(attribute.getID());

            if (!attribute. isValid(BuiltInRegistries. ITEM.getKey(item))) return;
            if (attribute.getWeight() <= 0 && !reforge) return;
            if (skipCursed && attribute.isCursed()) return;

            if (allowedGroups != null && !allowedGroups.isEmpty()) {
                String prefix = attrId.getPath().split("_")[0];
                if (! allowedGroups.contains(prefix.toLowerCase())) return;
            }

            if (group == null || attrId.getPath(). startsWith(group.toLowerCase() + "_")) {
                outAttributes. add(attrId);
                outWeights.add(reforge ? attribute.getWeight() + 1 : attribute.getWeight());
            }
        });
    }

    public static void setItemStackAttribute(@Nullable Player playerEntity, ItemStack stack, boolean reforge) {
        setItemStackAttribute(playerEntity, stack, reforge, null, false, false);
    }

    public static void setItemStackAttribute(@Nullable Player playerEntity, ItemStack stack, boolean reforge, @Nullable String group) {
        setItemStackAttribute(playerEntity, stack, reforge, group, false, false);
    }

    public static void setItemStackAttribute(@Nullable Player playerEntity, ItemStack stack, boolean reforge, @Nullable String group, boolean skipCursed) {
        setItemStackAttribute(playerEntity, stack, reforge, group, skipCursed, false);
    }

    public static void setItemStackAttribute(@Nullable Player playerEntity, ItemStack stack, boolean reforge, @Nullable String group, boolean skipCursed, boolean useAllowedGroupsFilter) {
        if (stack.get(Tiered.TIER) == null && !stack.is(TieredItemTags. MODIFIER_RESTRICTED)) {
            ResourceLocation potentialAttributeID = getRandomAttributeIDFor(playerEntity, stack.getItem(), reforge, group, skipCursed, useAllowedGroupsFilter);
            if (potentialAttributeID != null) {
                float durableFactor = -1f;
                int operation = 0;
                List<AttributeTemplate> attributeList = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(ResourceLocation.parse(potentialAttributeID.toString())).getAttributes();
                for (AttributeTemplate attributeTemplate : attributeList) {
                    if (attributeTemplate.getAttributeTypeID().equals("tiered:generic.durable")) {
                        durableFactor = (float) Math.round(attributeTemplate.getEntityAttributeModifier().amount() * 100.0f) / 100.0f;
                        operation = attributeTemplate.getEntityAttributeModifier().operation().id();
                        break;
                    }
                }
                stack.set(Tiered. TIER, new TierComponent(potentialAttributeID.toString(), durableFactor, operation));
            }
        }
    }

    public static void setItemStackAttribute(@Nullable Player playerEntity, ItemStack stack, boolean skipCursed, @Nullable ReforgeMaterial material) {
        setItemStackAttribute(playerEntity, stack, skipCursed, material, null);
    }

    public static void setItemStackAttribute(@Nullable Player playerEntity, ItemStack stack, boolean skipCursed,
            @Nullable ReforgeMaterial material, @Nullable ReforgeEffect.RollBias extraBias) {
        if (stack.get(Tiered.TIER) != null || stack.is(TieredItemTags.MODIFIER_RESTRICTED)) return;
        ResourceLocation potentialAttributeID;
        if (extraBias == null || extraBias.isEmpty()) {
            potentialAttributeID = getRandomAttributeIDFor(playerEntity, stack.getItem(), true, skipCursed, material);
        } else {
            potentialAttributeID = getRandomAttributeIDForWithBias(playerEntity, stack.getItem(), skipCursed, material, extraBias);
        }
        if (potentialAttributeID != null) {
            setItemStackAttributeWithId(stack, potentialAttributeID);
        }
    }

    private static ResourceLocation getRandomAttributeIDForWithBias(@Nullable Player playerEntity, Item item,
            boolean skipCursed, @Nullable ReforgeMaterial material, ReforgeEffect.RollBias extraBias) {
        List<ResourceLocation> potentialAttributes = new ArrayList<>();
        List<Integer> attributeWeights = new ArrayList<>();

        List<String> filterGroups = (material != null && material.hasGroupFilter()) ? material.getGroups() : null;
        collectValidAttributesForMaterial(item, true, skipCursed, filterGroups, material, potentialAttributes, attributeWeights);

        if (potentialAttributes.isEmpty() && filterGroups != null) {
            attributeWeights.clear();
            collectValidAttributesForMaterial(item, true, skipCursed, null, material, potentialAttributes, attributeWeights);
        }
        if (potentialAttributes.isEmpty()) return null;

        applyDynamicWeightModifiers(playerEntity, true, potentialAttributes, attributeWeights);

        float totalBoost = (material != null ? material.getRarityBoost() : 0f) + extraBias.extraRarityBoost;
        if (totalBoost > 0f) applyRarityBoost(potentialAttributes, attributeWeights, Math.min(1f, totalBoost));

        ResourceLocation result = weightedPick(potentialAttributes, attributeWeights);

        String matMin = material != null ? material.getGuaranteedMinRarity() : null;
        String biasMin = extraBias.guaranteedMinRarity;
        String effectiveMin = higherRarity(matMin, biasMin);

        if (result != null && effectiveMin != null) {
            int minOrder = ReforgeUtil.getRarityOrderExact(effectiveMin);
            if (minOrder >= 0 && rarityOrderOf(result) < minOrder) {
                List<ResourceLocation> qualifying = new ArrayList<>();
                List<Integer> qualifyingWeights = new ArrayList<>();
                for (int i = 0; i < potentialAttributes.size(); i++) {
                    if (rarityOrderOf(potentialAttributes.get(i)) >= minOrder) {
                        qualifying.add(potentialAttributes.get(i));
                        qualifyingWeights.add(attributeWeights.get(i));
                    }
                }
                if (!qualifying.isEmpty()) {
                    ResourceLocation upgraded = weightedPick(qualifying, qualifyingWeights);
                    if (upgraded != null) result = upgraded;
                }
            }
        }
        return result;
    }

    @Nullable
    private static String higherRarity(@Nullable String a, @Nullable String b) {
        if (a == null) return b;
        if (b == null) return a;
        return ReforgeUtil.getRarityOrderExact(a) >= ReforgeUtil.getRarityOrderExact(b) ? a : b;
    }

    public static void setItemStackAttributeWithId(ItemStack stack, ResourceLocation id) {
        PotentialAttribute attribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(id);
        if (attribute == null) return;

        float durableFactor = -1f;
        int operation = 0;

        for (AttributeTemplate template : attribute.getAttributes()) {
            if (template.getAttributeTypeID().equals("tiered:generic.durable")) {
                durableFactor = (float) Math.round(template.getEntityAttributeModifier().amount() * 100.0f) / 100.0f;
                operation = template.getEntityAttributeModifier().operation().id();
                break;
            }
        }

        stack.set(Tiered.TIER, new TierComponent(id.toString(), durableFactor, operation));
    }

    public static void removeItemStackAttribute(ItemStack itemStack) {
        if (itemStack.get(Tiered.TIER) != null) {
            itemStack. remove(Tiered.TIER);
        }
    }

    @Nullable
    public static ResourceLocation getAttributeId(ItemStack itemStack) {
        if (itemStack.get(Tiered.TIER) != null) {
            return ResourceLocation.parse(itemStack.get(Tiered.TIER).tier());
        }
        return null;
    }

    public static void updateItemStackComponent(Inventory playerInventory) {
        Map<ResourceLocation, PotentialAttribute> allAttributes = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes();
        if (allAttributes.isEmpty()) return;

        for (int u = 0; u < playerInventory.getContainerSize(); u++) {
            ItemStack itemStack = playerInventory.getItem(u);
            if (!itemStack.isEmpty() && itemStack.get(Tiered.TIER) != null) {

                String currentTier = itemStack.get(Tiered.TIER).tier();
                if (SPECIAL_TIER.equals(currentTier)) {
                    playerInventory.setItem(u, itemStack);
                    continue;
                }

                ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
                List<String> attributeIds = new ArrayList<>();
                for (PotentialAttribute attribute : allAttributes.values()) {
                    if (attribute.isValid(itemKey)) {
                        attributeIds.add(attribute.getID());
                    }
                }

                ResourceLocation attributeID = null;
                for (int i = 0; i < attributeIds.size(); i++) {
                    if (currentTier.contains(attributeIds.get(i))) {
                        attributeID = ResourceLocation.parse(attributeIds.get(i));
                        break;
                    } else if (i == attributeIds.size() - 1) {
                        ModifierUtils.removeItemStackAttribute(itemStack);
                        attributeID = ModifierUtils.getRandomAttributeIDFor(null, itemStack.getItem(), false, null, false, false);
                    }
                }

                if (attributeID != null) {
                    PotentialAttribute resolved = allAttributes.get(attributeID);
                    if (resolved == null) continue;

                    float durableFactor = -1f;
                    int operation = 0;
                    for (AttributeTemplate template : resolved.getAttributes()) {
                        if (template.getAttributeTypeID().equals("tiered:generic.durable")) {
                            durableFactor = (float) Math.round(template.getEntityAttributeModifier().amount() * 100.0f) / 100.0f;
                            operation = template.getEntityAttributeModifier().operation().id();
                            break;
                        }
                    }

                    itemStack.set(Tiered.TIER, new TierComponent(attributeID.toString(), durableFactor, operation));
                    playerInventory.setItem(u, itemStack);
                }
            }
        }
    }
}
