package draylar.tiered.api;

import draylar.tiered.Tiered;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.lib.SortList;
import net.levelz.access. LevelManagerAccess;
import net.levelz.level.LevelManager;
import net.levelz.level.Skill;
import net. minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net. minecraft.registry.Registries;
import net.minecraft.util. Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

public class ModifierUtils {

    private static final String SPECIAL_TIER = "tiered:special";

    @Nullable
    public static Identifier getRandomAttributeIDFor(@Nullable PlayerEntity playerEntity, Item item, boolean reforge, @Nullable String group, boolean skipCursed) {
        return getRandomAttributeIDFor(playerEntity, item, reforge, group, skipCursed, false);
    }

    @Nullable
    public static Identifier getRandomAttributeIDFor(@Nullable PlayerEntity playerEntity, Item item, boolean reforge, @Nullable String group, boolean skipCursed, boolean useAllowedGroupsFilter) {
        List<Identifier> potentialAttributes = new ArrayList<>();
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
            LevelManager.SKILLS.values().stream(). filter(skill -> skill.getKey().equals("smithing"));
            for (Skill skill : LevelManager.SKILLS.values()) {
                if (skill.getKey().equals("smithing")) {
                    int newMaxWeight = Collections.max(attributeWeights);
                    for (int i = 0; i < attributeWeights.size(); i++) {
                        if (attributeWeights.get(i) > newMaxWeight / 3) {
                            attributeWeights.set(i, (int) (attributeWeights.get(i)
                                    * (1.0f - ConfigInit.CONFIG.levelzReforgeModifier * ((LevelManagerAccess) playerEntity).getLevelManager().getSkillLevel(skill.getId()))));
                        }
                    }
                    break;
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
            PlayerEntity playerEntity,
            Item item,
            boolean reforge,
            @Nullable String group,
            boolean skipCursed,
            @Nullable List<String> allowedGroups,
            List<Identifier> outAttributes,
            List<Integer> outWeights
    ) {
        Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes(). forEach((id, attribute) -> {
            Identifier attrId = Identifier.of(attribute.getID());

            if (!attribute. isValid(Registries. ITEM.getId(item))) return;
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

    public static void setItemStackAttribute(@Nullable PlayerEntity playerEntity, ItemStack stack, boolean reforge) {
        setItemStackAttribute(playerEntity, stack, reforge, null, false, false);
    }

    public static void setItemStackAttribute(@Nullable PlayerEntity playerEntity, ItemStack stack, boolean reforge, @Nullable String group) {
        setItemStackAttribute(playerEntity, stack, reforge, group, false, false);
    }

    public static void setItemStackAttribute(@Nullable PlayerEntity playerEntity, ItemStack stack, boolean reforge, @Nullable String group, boolean skipCursed) {
        setItemStackAttribute(playerEntity, stack, reforge, group, skipCursed, false);
    }

    public static void setItemStackAttribute(@Nullable PlayerEntity playerEntity, ItemStack stack, boolean reforge, @Nullable String group, boolean skipCursed, boolean useAllowedGroupsFilter) {
        if (stack.get(Tiered.TIER) == null && !stack.isIn(TieredItemTags. MODIFIER_RESTRICTED)) {
            Identifier potentialAttributeID = getRandomAttributeIDFor(playerEntity, stack.getItem(), reforge, group, skipCursed, useAllowedGroupsFilter);
            if (potentialAttributeID != null) {
                float durableFactor = -1f;
                int operation = 0;
                List<AttributeTemplate> attributeList = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(Identifier.of(potentialAttributeID.toString())).getAttributes();
                for (AttributeTemplate attributeTemplate : attributeList) {
                    if (attributeTemplate.getAttributeTypeID(). equals("tiered:generic. durable")) {
                        durableFactor = (float) Math. round(attributeTemplate.getEntityAttributeModifier().value() * 100.0f) / 100.0f;
                        operation = attributeTemplate.getEntityAttributeModifier().operation().getId();
                        break;
                    }
                }
                stack.set(Tiered. TIER, new TierComponent(potentialAttributeID.toString(), durableFactor, operation));
            }
        }
    }

    public static void setItemStackAttributeWithId(ItemStack stack, Identifier id) {
        PotentialAttribute attribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(id);
        if (attribute == null) return;

        float durableFactor = -1f;
        int operation = 0;

        for (AttributeTemplate template : attribute.getAttributes()) {
            if (template.getAttributeTypeID().equals("tiered:generic.durable")) {
                durableFactor = (float) Math.round(template.getEntityAttributeModifier().value() * 100.0f) / 100.0f;
                operation = template.getEntityAttributeModifier().operation().getId();
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
    public static Identifier getAttributeId(ItemStack itemStack) {
        if (itemStack.get(Tiered.TIER) != null) {
            return Identifier.of(itemStack.get(Tiered.TIER).tier());
        }
        return null;
    }

    public static void updateItemStackComponent(PlayerInventory playerInventory) {
        for (int u = 0; u < playerInventory.size(); u++) {
            ItemStack itemStack = playerInventory.getStack(u);
            if (!itemStack. isEmpty() && itemStack.get(Tiered.TIER) != null) {

                String currentTier = itemStack.get(Tiered.TIER).tier();
                if (SPECIAL_TIER.equals(currentTier)) {
                    playerInventory. setStack(u, itemStack);
                    continue;
                }

                List<String> attributeIds = new ArrayList<>();
                Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
                    if (attribute.isValid(Registries.ITEM. getId(itemStack.getItem()))) {
                        attributeIds.add(attribute.getID());
                    }
                });

                Identifier attributeID = null;
                for (int i = 0; i < attributeIds.size(); i++) {
                    if (itemStack.get(Tiered.TIER).tier().contains(attributeIds.get(i))) {
                        attributeID = Identifier.of(attributeIds.get(i));
                        break;
                    } else if (i == attributeIds.size() - 1) {
                        ModifierUtils. removeItemStackAttribute(itemStack);
                        attributeID = ModifierUtils.getRandomAttributeIDFor(null, itemStack.getItem(), false, null, false, false);
                    }
                }

                if (attributeID != null) {
                    float durableFactor = -1f;
                    int operation = 0;
                    List<AttributeTemplate> attributeList = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(Identifier.of(attributeID.toString())).getAttributes();
                    for (int i = 0; i < attributeList.size(); i++) {
                        if (attributeList.get(i).getAttributeTypeID().equals("tiered:generic. durable")) {
                            durableFactor = (float) Math.round(attributeList.get(i).getEntityAttributeModifier().value() * 100.0f) / 100.0f;
                            operation = attributeList.get(i).getEntityAttributeModifier().operation(). getId();
                            break;
                        }
                    }

                    itemStack.set(Tiered.TIER, new TierComponent(attributeID.toString(), durableFactor, operation));
                    playerInventory.setStack(u, itemStack);
                }
            }
        }
    }
}