package draylar.tiered.reforge;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.ReforgeMaterial;
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
import draylar.tiered.network.TieredServerPacket;
import draylar.tiered.registry.ModComponents;
import draylar.tiered.registry.ModItems;
import draylar.tiered.registry.SpecialTuningIngotItem;
import draylar.tiered.util.ReforgeMaterials;
import draylar.tiered.util.ReforgeUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReforgeScreenHandler extends ScreenHandler {

    private final Inventory inventory = new SimpleInventory(3) {
        @Override
        public void markDirty() {
            super.markDirty();
            ReforgeScreenHandler.this.onContentChanged(this);
        }
    };

    private final ScreenHandlerContext context;
    private final PlayerEntity player;
    private boolean reforgeReady;
    private BlockPos pos;
    private boolean autoRefill = false;
    private Item lastUsedBaseMaterial = null;
    private Item lastUsedAddition = null;

    public ReforgeScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Tiered.REFORGE_SCREEN_HANDLER_TYPE, syncId);

        this.context = context;
        this.player = playerInventory.player;
        this.addSlot(new Slot(this.inventory, 0, 45, 47));
        this.addSlot(new Slot(this.inventory, 1, 80, 34));
        this.addSlot(new Slot(this.inventory, 2, 115, 47) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isIn(TieredItemTags.REFORGE_ADDITION);
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
        this.context.run((world, pos) -> {
            ReforgeScreenHandler.this.setPos(pos);
        });
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        super.onContentChanged(inventory);
        if (!player.getWorld().isClient() && inventory == this.inventory) {
            this.updateResult();
        }
    }

    private static boolean hasSpecialPrefixName(ItemStack stack) {
        Text overridden = stack.get(DataComponentTypes.ITEM_NAME);
        if (overridden == null) return false;
        return overridden.getString().startsWith("Special ");
    }

    private static void applySpecialName(ItemStack stack) {
        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) return;
        if (hasSpecialPrefixName(stack)) return;

        Text base = Text.translatable(stack.getItem().getTranslationKey());
        Text name = Text.literal("Special ")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(base.copy().styled(s -> s.withColor(Formatting.LIGHT_PURPLE).withItalic(false)));

        stack.set(DataComponentTypes.ITEM_NAME, name);
    }

    private void updateResult() {
        ItemStack stack = this.getSlot(1).getStack();
        ItemStack baseItem = this.getSlot(0).getStack();
        ItemStack addition = this.getSlot(2).getStack();

        this.reforgeReady = false;

        if (!addition.isEmpty() && addition.getItem() == ModItems.SPECIAL_TUNING_INGOT) {
            if (!ConfigInit.SPECIAL_INGOT_ENABLED || ConfigInit.SPECIAL_INGOT == null) {
                TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, true);
                return;
            }
            if (isBlockedForSpecial(stack)) {
                TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, true);
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
            boolean ready = !stack.isEmpty()
                    && ReforgeUtil.isMaterialCompatible(additionMaterial, stack)
                    && (additionMaterial.hasImprintPool() || ReforgeEffects.anyCanRun(readyEffects, stack));
            this.reforgeReady = ready;
            TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, !ready);
            return;
        }

        if (baseItem.isEmpty() || stack.isEmpty() || addition.isEmpty()) {
            TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, true);
            return;
        }

        if (stack.isIn(TieredItemTags.MODIFIER_RESTRICTED) || stack.isDamaged()) {
            TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, true);
            return;
        }

        Identifier currentAttributeId = ModifierUtils.getAttributeId(stack);
        if (currentAttributeId != null) {
            PotentialAttribute currentAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(currentAttributeId);
            if (currentAttribute != null && currentAttribute.isCursed()) {
                TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, true);
                return;
            }
        }

        Item item = stack.getItem();

        ReforgeMaterial material = ReforgeMaterials.resolve(addition);

        boolean rerolls = material == null || !material.isSkipsReforge();

        if (rerolls && ModifierUtils.getRandomAttributeIDFor(null, item, true, null, true) == null) {
            TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, true);
            return;
        }

        if (material != null) {
            if (!ReforgeUtil.isMaterialCompatible(material, stack)) {
                TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, true);
                return;
            }
            if (rerolls && ModifierUtils.getRandomAttributeIDFor(null, item, true, true, material) == null) {
                TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, true);
                return;
            }
        } else {
            String group = getGroupFromTuningIngot(addition);
            if (group != null) {
                if (ModifierUtils.getRandomAttributeIDFor(null, item, true, group, true) == null) {
                    TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, true);
                    return;
                }
            }
        }

        List<Item> validBaseItems = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(item);
        if (!validBaseItems.isEmpty()) {
            this.reforgeReady = validBaseItems.contains(baseItem.getItem());
        } else if (item instanceof ToolItem toolItem) {
            this.reforgeReady = toolItem.getMaterial().getRepairIngredient().test(baseItem);
        } else if (item instanceof ArmorItem armorItem && armorItem.getMaterial().value().repairIngredient() != null) {
            this.reforgeReady = armorItem.getMaterial().value().repairIngredient().get().test(baseItem);
        } else {
            this.reforgeReady = baseItem.isIn(TieredItemTags.REFORGE_BASE_ITEM);
        }

        if (this.reforgeReady
                && rerolls
                && !ConfigInit.CONFIG.uniqueReforge
                && ModifierUtils.getAttributeId(stack) != null
                && ModifierUtils.getAttributeId(stack).getPath().contains("unique")) {
            this.reforgeReady = false;
        }

        TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayerEntity) player, !this.reforgeReady);
    }

    private boolean isBlockedForSpecial(ItemStack target) {
        var cfg = ConfigInit.SPECIAL_INGOT;
        if (cfg == null) return false;

        if (cfg.blockedItemIds != null && !cfg.blockedItemIds.isEmpty()) {
            String id = Registries.ITEM.getId(target.getItem()).toString();
            for (String s : cfg.blockedItemIds) {
                if (s.equalsIgnoreCase(id)) return true;
            }
        }

        if (cfg.blockedItemTags != null && !cfg.blockedItemTags.isEmpty()) {
            for (String t : cfg.blockedItemTags) {
                String raw = t.startsWith("#") ? t.substring(1) : t;
                var tagId = Identifier.of(raw);
                var tag = TagKey.of(Registries.ITEM.getKey(), tagId);
                if (target.isIn(tag)) return true;
            }
        }

        return false;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.context.run((world, pos) -> this.dropInventory(player, this.inventory));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.context.get((world, pos) -> {
            return player.squaredDistanceTo((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    @Nullable
    private String getGroupFromTuningIngot(ItemStack tuningIngot) {
        if (tuningIngot.isEmpty()) return null;

        Identifier id = Registries.ITEM.getId(tuningIngot.getItem());
        String path = id.getPath();

        if (path.startsWith("tuning_ingot_")) {
            return path.substring("tuning_ingot_".length()).toLowerCase();
        }

        return null;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack itemStack2 = slot.getStack();
            itemStack = itemStack2.copy();
            if (index == 1) {
                if (!this.insertItem(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(itemStack2, itemStack);
            } else if (index == 0 || index == 2) {
                if (!this.insertItem(itemStack2, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 3 && index < 39) {
                if (itemStack.isIn(TieredItemTags.REFORGE_ADDITION) && !this.insertItem(itemStack2, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }
                if (this.getSlot(1).hasStack()) {
                    Item item = this.getSlot(1).getStack().getItem();
                    if (item instanceof ToolItem toolItem && toolItem.getMaterial().getRepairIngredient().test(itemStack) && !this.insertItem(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    if (item instanceof ArmorItem armorItem && armorItem.getMaterial().value().repairIngredient() != null && armorItem.getMaterial().value().repairIngredient().get().test(itemStack)
                            && !this.insertItem(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    if (itemStack.isIn(TieredItemTags.REFORGE_BASE_ITEM) && !this.insertItem(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(item);
                    if (items.stream().anyMatch(it -> it == itemStack2.copy().getItem()) && !this.insertItem(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                if (ModifierUtils.getRandomAttributeIDFor(null, itemStack.getItem(), false, null, true) != null && !this.insertItem(itemStack2, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (itemStack2.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTakeItem(player, itemStack2);
        }
        return itemStack;
    }

    public void reforge() {
        ItemStack itemStack = this.getSlot(1).getStack();
        ItemStack tuningIngot = this.getSlot(2).getStack();

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

            boolean changed = ReforgeEffects.run(player, itemStack, standaloneEffects,
                    noReforgeMaterial.getEffectParams(), player.getWorld(), this.pos, tuningIngot);

            if (!changed && noReforgeMaterial.hasImprintPool()) {
                RuneContent.rollOnto(tuningIngot, null);
                changed = Imprints.grantFromContent(itemStack, tuningIngot);
            }
            if (!changed) return;

            this.decrementStack(2);
            this.context.run((world, pos) -> world.syncWorldEvent(WorldEvents.ANVIL_USED, this.pos, 0));
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
                this.context.run((world, pos) -> world.syncWorldEvent(WorldEvents.ANVIL_USED, this.pos, 0));
                return;
            }
        }

        ReforgeMaterial material = ReforgeMaterials.resolve(tuningIngot);
        boolean keepTier = material != null && material.isSkipsReforge();

        if (!keepTier && material == null) {
            ModifierUtils.removeItemStackAttribute(itemStack);
            itemStack.remove(ModComponents.SPECIAL_STATS);
            if (hasSpecialPrefixName(itemStack)) {
                itemStack.remove(DataComponentTypes.ITEM_NAME);
            }
        }

        if (material != null) {
            if (!ReforgeUtil.isMaterialCompatible(material, itemStack)) {
                return;
            }

            RuneContentComponent runeComp = tuningIngot.get(ModComponents.RUNE_CONTENT);
            List<String> effectIds = (runeComp != null && !runeComp.rolledEffects().isEmpty())
                    ? mergeEffects(material.getEffects(), runeComp.rolledEffects())
                    : material.getEffects();
            Map<String, Map<String, Float>> effectParams =
                    mergeEffectParams(material.getEffectParams(), runeComp != null ? runeComp.rolledEffectParams() : null);
            if (!keepTier) {

                ReforgeEffect.RollBias rollBias = ReforgeEffects.preReforge(
                        player, itemStack, effectIds, effectParams,
                        player.getWorld(), this.pos, tuningIngot);

                ModifierUtils.removeItemStackAttribute(itemStack);
                itemStack.remove(ModComponents.SPECIAL_STATS);
                if (hasSpecialPrefixName(itemStack)) {
                    itemStack.remove(DataComponentTypes.ITEM_NAME);
                }

                for (int i = 0; i < rollBias.extraBaseCost; i++) {
                    this.decrementStack(0);
                }
                ModifierUtils.setItemStackAttribute(player, itemStack, true, material, rollBias);
            }

            ReforgeEffects.run(player, itemStack, effectIds, effectParams,
                    player.getWorld(), this.pos, tuningIngot);

            Imprints.grantFromContent(itemStack, tuningIngot);
        } else {
            String group = getGroupFromTuningIngot(tuningIngot);
            ModifierUtils.setItemStackAttribute(player, itemStack, true, group, true, false);
        }

        this.decrementStack(0);
        this.decrementStack(2);
        this.context.run((world, pos) -> world.syncWorldEvent(WorldEvents. ANVIL_USED, this. pos, 0));
    }

    public void performExtract(int chosenIndex) {
        ItemStack itemStack = this.getSlot(1).getStack();
        ItemStack tuningIngot = this.getSlot(2).getStack();
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
                if (!player.getInventory().insertStack(returned)) {
                    player.dropItem(returned, false);
                }
            }
        }
        this.context.run((world, pos) -> world.syncWorldEvent(WorldEvents.ANVIL_USED, this.pos, 0));
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
        String itemId = Registries.ITEM.getId(rune.getItem()).toString();
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
        ItemStack itemStack = this.inventory.getStack(slot);

        if (slot == 0 && !itemStack.isEmpty()) {
            lastUsedBaseMaterial = itemStack.getItem();
        } else if (slot == 2 && !itemStack.isEmpty()) {
            lastUsedAddition = itemStack.getItem();
        }

        itemStack.decrement(1);
        this.inventory.setStack(slot, itemStack);

        if (autoRefill && itemStack.getCount() < 16) {
            boolean refilled = tryRefillSlot(slot);
            if (!refilled && itemStack.isEmpty() && player instanceof ServerPlayerEntity serverPlayer) {
                TieredServerPacket.writeS2CStopAutoReforgePacket(serverPlayer);
            }
        }
    }

    private boolean tryRefillSlot(int slot) {
        ItemStack slotStack = this.inventory.getStack(slot);
        int currentCount = slotStack.isEmpty() ? 0 : slotStack.getCount();
        int targetCount = 32;
        int needed = targetCount - currentCount;
        if (needed <= 0) return true;

        if (slot == 0) {
            if (lastUsedBaseMaterial == null) return currentCount > 0;

            ItemStack targetItem = this.inventory.getStack(1);
            if (targetItem.isEmpty()) return currentCount > 0;

            List<Item> validBaseItems = getValidBaseItems(targetItem);
            if (validBaseItems.isEmpty() || !validBaseItems.contains(lastUsedBaseMaterial)) return currentCount > 0;

            int gathered = 0;
            for (int i = 3; i < this.slots.size() && gathered < needed; i++) {
                ItemStack playerStack = this.slots.get(i).getStack();
                if (!playerStack.isEmpty() && playerStack.getItem() == lastUsedBaseMaterial) {
                    int toTake = Math.min(playerStack.getCount(), needed - gathered);
                    if (slotStack.isEmpty()) {
                        slotStack = new ItemStack(lastUsedBaseMaterial, toTake);
                        this.inventory.setStack(slot, slotStack);
                    } else {
                        slotStack.increment(toTake);
                    }
                    playerStack.decrement(toTake);
                    gathered += toTake;
                }
            }
            return gathered > 0 || currentCount > 0;
        } else if (slot == 2) {
            if (lastUsedAddition == null) return currentCount > 0;

            int gathered = 0;
            for (int i = 3; i < this.slots.size() && gathered < needed; i++) {
                ItemStack playerStack = this.slots.get(i).getStack();
                if (!playerStack.isEmpty() && playerStack.getItem() == lastUsedAddition) {
                    int toTake = Math.min(playerStack.getCount(), needed - gathered);
                    if (slotStack.isEmpty()) {
                        slotStack = new ItemStack(lastUsedAddition, toTake);
                        this.inventory.setStack(slot, slotStack);
                    } else {
                        slotStack.increment(toTake);
                    }
                    playerStack.decrement(toTake);
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
        } else if (item instanceof ToolItem toolItem) {
            for (ItemStack s : toolItem.getMaterial().getRepairIngredient().getMatchingStacks()) {
                validItems.add(s.getItem());
            }
        } else if (item instanceof ArmorItem armorItem && armorItem.getMaterial().value().repairIngredient() != null) {
            for (ItemStack s : armorItem.getMaterial().value().repairIngredient().get().getMatchingStacks()) {
                validItems.add(s.getItem());
            }
        }

        return validItems;
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return slot.inventory != this.inventory && super.canInsertIntoSlot(stack, slot);
    }
}
