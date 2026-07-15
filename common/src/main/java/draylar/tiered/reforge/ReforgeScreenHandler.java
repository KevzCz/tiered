package draylar.tiered.reforge;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.api.SpecialStatsComponent;
import draylar.tiered.api.effect.DataEffect;
import draylar.tiered.api.effect.ReforgeEffect;
import draylar.tiered.api.effect.ReforgeEffects;
import draylar.tiered.api.TierComponent;
import draylar.tiered.api.TieredItemTags;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.Imprints;
import draylar.tiered.api.imprint.RuneContent;
import draylar.tiered.api.imprint.RuneContentComponent;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.SpecialIngotConfig;
import draylar.tiered.network.TieredServerPacket;
import draylar.tiered.registry.ModComponents;
import draylar.tiered.registry.ModItems;
import draylar.tiered.registry.SpecialTuningIngotItem;
import draylar.tiered.util.ReforgeMaterials;
import draylar.tiered.util.ReforgeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.LevelEvent;

public class ReforgeScreenHandler extends AbstractContainerMenu {

    private final Container inventory = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ReforgeScreenHandler.this.slotsChanged(this);
        }
    };

    private final ContainerLevelAccess context;
    private final Player player;
    private boolean reforgeReady;
    private BlockPos pos;
    private boolean autoRefill = false;
    private Item lastUsedBaseMaterial = null;
    private Item lastUsedAddition = null;

    public ReforgeScreenHandler(int syncId, Inventory playerInventory, ContainerLevelAccess context) {
        super(Tiered.REFORGE_SCREEN_HANDLER_TYPE, syncId);

        this.context = context;
        this.player = playerInventory.player;
        this.addSlot(new Slot(this.inventory, 0, 45, 47));
        this.addSlot(new Slot(this.inventory, 1, 80, 34));
        this.addSlot(new Slot(this.inventory, 2, 115, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(TieredItemTags.REFORGE_ADDITION);
            }
        });

        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        this.context.execute((world, pos) -> {
            ReforgeScreenHandler.this.setPos(pos);
        });
    }

    @Override
    public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (!player.level().isClientSide() && inventory == this.inventory) {
            this.updateResult();
        }
    }

    private static boolean hasSpecialPrefixName(ItemStack stack) {
        Component overridden = stack.get(DataComponents.ITEM_NAME);
        if (overridden == null) return false;
        return overridden.getString().startsWith("Special ");
    }

    private static void applySpecialName(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_NAME)) return;
        if (hasSpecialPrefixName(stack)) return;

        Component base = Component.translatable(stack.getItem().getDescriptionId());
        Component name = Component.literal("Special ")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(base.copy().withStyle(s -> s.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(false)));

        stack.set(DataComponents.ITEM_NAME, name);
    }

    private void updateResult() {
        ItemStack stack = this.getSlot(1).getItem();
        ItemStack baseItem = this.getSlot(0).getItem();
        ItemStack addition = this.getSlot(2).getItem();

        this.reforgeReady = false;

        if (!addition.isEmpty() && addition.getItem() == ModItems.SPECIAL_TUNING_INGOT) {
            if (!ConfigInit.SPECIAL_INGOT_ENABLED || ConfigInit.SPECIAL_INGOT == null) {
                TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, true);
                return;
            }
            if (isBlockedForSpecial(stack)) {
                TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, true);
                return;
            }
        }

        ReforgeMaterial additionMaterial = addition.isEmpty() ? null : ReforgeMaterials.resolve(addition);
        if (additionMaterial != null && additionMaterial.isSkipsBaseItem()) {
            RuneContentComponent readyComp =
                    addition.get(ModComponents.RUNE_CONTENT);
            List<String> readyEffects = (readyComp != null && !readyComp.rolledEffects().isEmpty())
                    ? mergeEffects(additionMaterial.getEffects(), readyComp.rolledEffects())
                    : additionMaterial.getEffects();
            boolean imprintReady = ConfigInit.imprintsEffectsAndBehaviorsEnabled() && additionMaterial.hasImprintPool();
            boolean effectsReady = ConfigInit.imprintsEffectsAndBehaviorsEnabled() && ReforgeEffects.anyCanRun(readyEffects, stack);
            boolean ready = !stack.isEmpty()
                    && ReforgeUtil.isMaterialCompatible(additionMaterial, stack)
                    && (imprintReady || effectsReady);
            this.reforgeReady = ready;
            TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, !ready);
            return;
        }

        if (baseItem.isEmpty() || stack.isEmpty() || addition.isEmpty()) {
            TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, true);
            return;
        }

        if (stack.is(TieredItemTags.MODIFIER_RESTRICTED) || stack.isDamaged()) {
            TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, true);
            return;
        }

        ResourceLocation currentAttributeId = ModifierUtils.getAttributeId(stack);
        if (currentAttributeId != null) {
            PotentialAttribute currentAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(currentAttributeId);
            if (currentAttribute != null && currentAttribute.isCursed()) {
                TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, true);
                return;
            }
        }

        Item item = stack.getItem();

        ReforgeMaterial material = ReforgeMaterials.resolve(addition);

        boolean rerolls = material == null || !material.isSkipsReforge();

        if (rerolls && ModifierUtils.getRandomAttributeIDFor(null, item, true, null, true) == null) {
            TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, true);
            return;
        }

        if (material != null) {
            if (!ReforgeUtil.isMaterialCompatible(material, stack)) {
                TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, true);
                return;
            }
            if (rerolls && ModifierUtils.getRandomAttributeIDFor(null, item, true, true, material) == null) {
                TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, true);
                return;
            }
        } else {
            String group = getGroupFromTuningIngot(addition);
            if (group != null) {
                if (ModifierUtils.getRandomAttributeIDFor(null, item, true, group, true) == null) {
                    TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, true);
                    return;
                }
            }
        }

        List<Item> validBaseItems = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(item);
        if (!validBaseItems.isEmpty()) {
            this.reforgeReady = validBaseItems.contains(baseItem.getItem());
        } else if (item instanceof TieredItem toolItem) {
            this.reforgeReady = toolItem.getTier().getRepairIngredient().test(baseItem);
        } else if (item instanceof ArmorItem armorItem && armorItem.getMaterial().value().repairIngredient() != null) {
            this.reforgeReady = armorItem.getMaterial().value().repairIngredient().get().test(baseItem);
        } else {
            this.reforgeReady = baseItem.is(TieredItemTags.REFORGE_BASE_ITEM);
        }

        if (this.reforgeReady
                && rerolls
                && !ConfigInit.CONFIG.uniqueReforge
                && ModifierUtils.getAttributeId(stack) != null
                && ModifierUtils.getAttributeId(stack).getPath().contains("unique")) {
            this.reforgeReady = false;
        }

        TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, !this.reforgeReady);
    }

    private boolean isBlockedForSpecial(ItemStack target) {
        var cfg = ConfigInit.SPECIAL_INGOT;
        if (cfg == null) return false;

        if (cfg.blockedItemIds != null && !cfg.blockedItemIds.isEmpty()) {
            String id = BuiltInRegistries.ITEM.getKey(target.getItem()).toString();
            for (String s : cfg.blockedItemIds) {
                if (s.equalsIgnoreCase(id)) return true;
            }
        }

        if (cfg.blockedItemTags != null && !cfg.blockedItemTags.isEmpty()) {
            for (String t : cfg.blockedItemTags) {
                String raw = t.startsWith("#") ? t.substring(1) : t;
                var tagId = ResourceLocation.parse(raw);
                var tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
                if (target.is(tag)) return true;
            }
        }

        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.context.execute((world, pos) -> this.clearContainer(player, this.inventory));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.context.evaluate((world, pos) -> {
            return player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    @Nullable
    private String getGroupFromTuningIngot(ItemStack tuningIngot) {
        if (tuningIngot.isEmpty()) return null;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(tuningIngot.getItem());
        String path = id.getPath();

        if (path.startsWith("tuning_ingot_")) {
            return path.substring("tuning_ingot_".length()).toLowerCase();
        }

        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (index == 1) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index == 0 || index == 2) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 3 && index < 39) {
                if (itemStack.is(TieredItemTags.REFORGE_ADDITION) && !this.moveItemStackTo(itemStack2, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }
                if (this.getSlot(1).hasItem()) {
                    Item item = this.getSlot(1).getItem().getItem();
                    if (item instanceof TieredItem toolItem && toolItem.getTier().getRepairIngredient().test(itemStack) && !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    if (item instanceof ArmorItem armorItem && armorItem.getMaterial().value().repairIngredient() != null && armorItem.getMaterial().value().repairIngredient().get().test(itemStack)
                            && !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    if (itemStack.is(TieredItemTags.REFORGE_BASE_ITEM) && !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(item);
                    if (items.stream().anyMatch(it -> it == itemStack2.copy().getItem()) && !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                if (ModifierUtils.getRandomAttributeIDFor(null, itemStack.getItem(), false, null, true) != null && !this.moveItemStackTo(itemStack2, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, itemStack2);
        }
        return itemStack;
    }

    public void reforge() {
        ItemStack itemStack = this.getSlot(1).getItem();
        ItemStack tuningIngot = this.getSlot(2).getItem();

        ReforgeMaterial noReforgeMaterial = tuningIngot.isEmpty() ? null : ReforgeMaterials.resolve(tuningIngot);
        if (noReforgeMaterial != null && noReforgeMaterial.isSkipsBaseItem()) {
            if (itemStack.isEmpty() || !ReforgeUtil.isMaterialCompatible(noReforgeMaterial, itemStack)) return;

            RuneContentComponent standaloneComp =
                    tuningIngot.get(ModComponents.RUNE_CONTENT);
            List<String> standaloneEffects = (standaloneComp != null && !standaloneComp.rolledEffects().isEmpty())
                    ? mergeEffects(noReforgeMaterial.getEffects(), standaloneComp.rolledEffects())
                    : noReforgeMaterial.getEffects();

            DataEffect extract = ReforgeEffects.findExtract(standaloneEffects);
            if (extract != null) {
                performExtract(-1);
                return;
            }

            boolean imprintsEnabled = ConfigInit.imprintsEffectsAndBehaviorsEnabled();
            boolean changed = imprintsEnabled && ReforgeEffects.run(player, itemStack, standaloneEffects,
                    noReforgeMaterial.getEffectParams(), player.level(), this.pos, tuningIngot);

            if (imprintsEnabled && !changed && noReforgeMaterial.hasImprintPool()) {
                RuneContent.rollOnto(tuningIngot, null);
                changed = Imprints.grantFromContent(itemStack, tuningIngot);
            }
            if (!changed) return;

            this.decrementStack(2);
            this.context.execute((world, pos) -> world.levelEvent(LevelEvent.SOUND_ANVIL_USED, this.pos, 0));
            return;
        }

        if (tuningIngot.getItem() == ModItems.SPECIAL_TUNING_INGOT) {
            if (! ConfigInit.SPECIAL_INGOT_ENABLED || ConfigInit.SPECIAL_INGOT == null) {
                return;
            }
            SpecialTuningIngotItem. ensureRoll(tuningIngot);
            var comp = tuningIngot.get(ModComponents.SPECIAL_STATS);
            if (comp != null) {
                ModifierUtils.removeItemStackAttribute(itemStack);
                if (! hasSpecialPrefixName(itemStack)) applySpecialName(itemStack);

                itemStack.set(Tiered.TIER, new TierComponent("tiered:special", -1f, 0));
                itemStack.set(ModComponents. SPECIAL_STATS, comp);

                this.decrementStack(0);
                this.decrementStack(2);
                this.context.execute((world, pos) -> world.levelEvent(LevelEvent.SOUND_ANVIL_USED, this.pos, 0));
                return;
            }
        }

        ReforgeMaterial material = ReforgeMaterials.resolve(tuningIngot);
        boolean keepTier = material != null && material.isSkipsReforge();

        if (!keepTier && material == null) {
            ModifierUtils.removeItemStackAttribute(itemStack);
            itemStack.remove(ModComponents.SPECIAL_STATS);
            if (hasSpecialPrefixName(itemStack)) {
                itemStack.remove(DataComponents.ITEM_NAME);
            }
        }

        if (material != null) {
            if (!ReforgeUtil.isMaterialCompatible(material, itemStack)) {
                return;
            }

            boolean imprintsEnabled = ConfigInit.imprintsEffectsAndBehaviorsEnabled();
            RuneContentComponent runeComp = tuningIngot.get(ModComponents.RUNE_CONTENT);
            List<String> effectIds = !imprintsEnabled ? List.of()
                    : (runeComp != null && !runeComp.rolledEffects().isEmpty())
                    ? mergeEffects(material.getEffects(), runeComp.rolledEffects())
                    : material.getEffects();
            Map<String, Map<String, Float>> effectParams = !imprintsEnabled ? Map.of()
                    : mergeEffectParams(material.getEffectParams(), runeComp != null ? runeComp.rolledEffectParams() : null);
            if (!keepTier) {

                ReforgeEffect.RollBias rollBias = ReforgeEffects.preReforge(
                        player, itemStack, effectIds, effectParams,
                        player.level(), this.pos, tuningIngot);

                ModifierUtils.removeItemStackAttribute(itemStack);
                itemStack.remove(ModComponents.SPECIAL_STATS);
                if (hasSpecialPrefixName(itemStack)) {
                    itemStack.remove(DataComponents.ITEM_NAME);
                }

                for (int i = 0; i < rollBias.extraBaseCost; i++) {
                    this.decrementStack(0);
                }
                ModifierUtils.setItemStackAttribute(player, itemStack, true, material, rollBias);
            }

            ReforgeEffects.run(player, itemStack, effectIds, effectParams,
                    player.level(), this.pos, tuningIngot);

            if (imprintsEnabled) {
                Imprints.grantFromContent(itemStack, tuningIngot);
            }
        } else {
            String group = getGroupFromTuningIngot(tuningIngot);
            ModifierUtils.setItemStackAttribute(player, itemStack, true, group, true, false);
        }

        this.decrementStack(0);
        this.decrementStack(2);
        this.context.execute((world, pos) -> world.levelEvent(LevelEvent. SOUND_ANVIL_USED, this. pos, 0));
    }

    public void performExtract(int chosenIndex) {
        ItemStack itemStack = this.getSlot(1).getItem();
        ItemStack tuningIngot = this.getSlot(2).getItem();
        if (itemStack.isEmpty() || tuningIngot.isEmpty()) return;

        ReforgeMaterial material = ReforgeMaterials.resolve(tuningIngot);
        RuneContentComponent extractComp =
                tuningIngot.get(ModComponents.RUNE_CONTENT);
        List<String> extractEffects = material == null ? List.of()
                : ((extractComp != null && !extractComp.rolledEffects().isEmpty())
                        ? mergeEffects(material.getEffects(), extractComp.rolledEffects())
                        : material.getEffects());
        DataEffect extract = ReforgeEffects.findExtract(extractEffects);
        if (extract == null) return;
        if (!ReforgeUtil.isMaterialCompatible(material, itemStack)) return;
        if (Imprints.slotsUsed(itemStack) <= 0) return;

        int index = chosenIndex < 0 ? Imprints.slotsUsed(itemStack) - 1 : chosenIndex;
        boolean success = player.getRandom().nextFloat() < extract.definition().getValue();

        ItemStack consumed = tuningIngot.copy();
        this.decrementStack(2);
        if (success) {
            var group = Imprints.extractSlot(itemStack, index);
            if (!group.isEmpty()) {
                ItemStack returned = consumed.copyWithCount(1);
                RuneContent.writeContent(returned, group);
                ReforgeMaterials.setOverride(returned, bareImprintMaterial(returned, group));
                if (!player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
            }
        }
        this.context.execute((world, pos) -> world.levelEvent(LevelEvent.SOUND_ANVIL_USED, this.pos, 0));
    }

    private static Map<String, Map<String, Float>> mergeEffectParams(
            @Nullable Map<String, Map<String, Float>> base,
            @Nullable Map<String, Map<String, Float>> rolled) {
        if (base == null && rolled == null) return Map.of();
        if (base == null) return rolled;
        if (rolled == null || rolled.isEmpty()) return base;
        Map<String, Map<String, Float>> merged = new LinkedHashMap<>(base);
        for (var e : rolled.entrySet()) {
            merged.merge(e.getKey(), e.getValue(), (a, b) -> {
                Map<String, Float> combined = new LinkedHashMap<>(a);
                combined.putAll(b);
                return combined;
            });
        }
        return merged;
    }

    private static List<String> mergeEffects(@Nullable List<String> base, List<String> extra) {
        if (base == null || base.isEmpty()) return extra;
        List<String> merged = new ArrayList<>(base);
        merged.addAll(extra);
        return merged;
    }

    private static ReforgeMaterial bareImprintMaterial(ItemStack rune, List<ImprintComponent.Entry> group) {
        List<ReforgeMaterial.Candidate> candidates = new ArrayList<>();
        for (ImprintComponent.Entry e : group) {
            candidates.add(new ReforgeMaterial.Candidate(e.id(), e.value(), e.value(), 1));
        }
        ReforgeMaterial.ImprintPool pool = new ReforgeMaterial.ImprintPool(group.size(), group.size(), candidates, null, null, null, null);
        String itemId = BuiltInRegistries.ITEM.getKey(rune.getItem()).toString();
        return new ReforgeMaterial(itemId, null, null, null, null, null, null, null, null, null, null, null, pool, true, true);
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public void setAutoRefill(boolean enabled) {
        this.autoRefill = enabled;
    }

    public boolean isAutoRefill() {
        return this.autoRefill;
    }

    private void decrementStack(int slot) {
        ItemStack itemStack = this.inventory.getItem(slot);

        if (slot == 0 && !itemStack.isEmpty()) {
            lastUsedBaseMaterial = itemStack.getItem();
        } else if (slot == 2 && !itemStack.isEmpty()) {
            lastUsedAddition = itemStack.getItem();
        }

        itemStack.shrink(1);
        this.inventory.setItem(slot, itemStack);

        if (autoRefill && itemStack.getCount() < 16) {
            boolean refilled = tryRefillSlot(slot);
            if (!refilled && itemStack.isEmpty() && player instanceof ServerPlayer serverPlayer) {
                TieredServerPacket.writeS2CStopAutoReforgePacket(serverPlayer);
            }
        }
    }

    private boolean tryRefillSlot(int slot) {
        ItemStack slotStack = this.inventory.getItem(slot);
        int currentCount = slotStack.isEmpty() ? 0 : slotStack.getCount();
        int targetCount = 32;
        int needed = targetCount - currentCount;
        if (needed <= 0) return true;

        if (slot == 0) {
            if (lastUsedBaseMaterial == null) return currentCount > 0;

            ItemStack targetItem = this.inventory.getItem(1);
            if (targetItem.isEmpty()) return currentCount > 0;

            List<Item> validBaseItems = getValidBaseItems(targetItem);
            if (validBaseItems.isEmpty() || !validBaseItems.contains(lastUsedBaseMaterial)) return currentCount > 0;

            int gathered = 0;
            for (int i = 3; i < this.slots.size() && gathered < needed; i++) {
                ItemStack playerStack = this.slots.get(i).getItem();
                if (!playerStack.isEmpty() && playerStack.getItem() == lastUsedBaseMaterial) {
                    int toTake = Math.min(playerStack.getCount(), needed - gathered);
                    if (slotStack.isEmpty()) {
                        slotStack = new ItemStack(lastUsedBaseMaterial, toTake);
                        this.inventory.setItem(slot, slotStack);
                    } else {
                        slotStack.grow(toTake);
                    }
                    playerStack.shrink(toTake);
                    gathered += toTake;
                }
            }
            return gathered > 0 || currentCount > 0;
        } else if (slot == 2) {
            if (lastUsedAddition == null) return currentCount > 0;

            int gathered = 0;
            for (int i = 3; i < this.slots.size() && gathered < needed; i++) {
                ItemStack playerStack = this.slots.get(i).getItem();
                if (!playerStack.isEmpty() && playerStack.getItem() == lastUsedAddition) {
                    int toTake = Math.min(playerStack.getCount(), needed - gathered);
                    if (slotStack.isEmpty()) {
                        slotStack = new ItemStack(lastUsedAddition, toTake);
                        this.inventory.setItem(slot, slotStack);
                    } else {
                        slotStack.grow(toTake);
                    }
                    playerStack.shrink(toTake);
                    gathered += toTake;
                }
            }
            return gathered > 0 || currentCount > 0;
        }
        return currentCount > 0;
    }

    private List<Item> getValidBaseItems(ItemStack targetItem) {
        List<Item> validItems = new ArrayList<>();
        Item item = targetItem.getItem();

        List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(item);
        if (!items.isEmpty()) {
            validItems.addAll(items);
        } else if (item instanceof TieredItem toolItem) {
            for (ItemStack s : toolItem.getTier().getRepairIngredient().getItems()) {
                validItems.add(s.getItem());
            }
        } else if (item instanceof ArmorItem armorItem && armorItem.getMaterial().value().repairIngredient() != null) {
            for (ItemStack s : armorItem.getMaterial().value().repairIngredient().get().getItems()) {
                validItems.add(s.getItem());
            }
        }

        return validItems;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.inventory && super.canTakeItemForPickAll(stack, slot);
    }
}
