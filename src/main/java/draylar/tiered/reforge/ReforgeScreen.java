package draylar.tiered.reforge;

import com.mojang.blaze3d.systems.RenderSystem;
import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.TieredItemTags;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.network.TieredClientPacket;
import draylar.tiered.util.ReforgeUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.libz.api.Tab;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import draylar.tiered.util.ReforgeUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static draylar.tiered.util.ReforgeUtil.formatModifierName;

@Environment(EnvType.CLIENT)
public class ReforgeScreen extends HandledScreen<ReforgeScreenHandler> implements ScreenHandlerListener, Tab {
    @Nullable
    private Identifier targetModifier = null;
    private boolean modifierAchieved = false;
    public static final Identifier TEXTURE = Identifier.of("tiered", "textures/gui/reforging_screen.png");
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private class FloatingText {
        final Text text;
        final int baseColor;
        int age = 0;
        final int maxAge = 50;
        final int x, y;

        FloatingText(Text text, int color, int x, int y) {
            this.text = text;
            this.baseColor = color;
            this.x = x;
            this.y = y;
        }

        void tick() {
            age++;
        }

        boolean isExpired() {
            return age >= maxAge;
        }

        float getAlpha() {
            if (age < 10) {

                float t = age / 10f;
                return t * t;
            } else if (age < 30) {

                return 1f;
            } else if (age < maxAge) {

                float t = (maxAge - age) / 20f;
                return t * t;
            } else {
                return 0f;
            }
        }

        float getYOffset() {

            float t = age / (float) maxAge;
            return (float) (-Math.pow(t, 0.6) * 20.0);
        }

        int getRenderColor() {
            int alpha = (int) (getAlpha() * 255);
            return (alpha << 24) | baseColor;
        }
    }


    private Identifier lastSeenModifier = null;
    private ItemStack lastSeenStack = ItemStack.EMPTY;

    public ReforgeButton reforgeButton;
    private ClickableWidget showModifiersButton;

    private ItemStack last;
    private List<Item> baseItems;


    private boolean modifiersVisible = false;
    private final Map<String, List<Identifier>> groupedModifiers = new LinkedHashMap<>();
    private final Set<String> expandedGroups = new HashSet<>();
    private int scrollOffset = 0;
    private final int entryHeight = 12;
    private final int maxVisibleEntries = 12;
    private final List<Identifier> ungroupedModifiers = new ArrayList<>();

    public ReforgeScreen(ReforgeScreenHandler handler, PlayerInventory playerInventory, Text title) {
        super(handler, playerInventory, title);
        this.titleX = 60;
    }

    @Override
    protected void init() {
        super.init();
        handler.addListener(this);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        this.reforgeButton = this.addDrawableChild(new ReforgeButton(x + 79, y + 56, (button) -> {
            if (!reforgeButton.disabled && !modifierAchieved) {
                TieredClientPacket.writeC2SReforgePacket();
            }
        }));



        int iconX = ConfigInit.CONFIG.leftSideModifierList ? x + 5 : x + 155;
        int iconY = y + 5;
        this.showModifiersButton = new ClickableWidget(iconX, iconY, 16, 16, Text.empty()) {

            @Override
            public void onClick(double mouseX, double mouseY) {
                modifiersVisible = !modifiersVisible;
                scrollOffset = 0;
                groupedModifiers.clear();
                expandedGroups.clear();
                ungroupedModifiers.clear();

                if (modifiersVisible) {
                    ItemStack stack = handler.getSlot(1).getStack();
                    if (!stack.isEmpty()) {
                        List<Identifier> modifiers = ReforgeUtil.getAvailableModifiers(stack);
                        modifiers.sort(Comparator
                                .comparing(ReforgeUtil::getRarityOrder)
                                .thenComparing(ReforgeUtil::getNumericSuffixOrZero)
                        );


                        Map<String, Integer> prefixCount = new HashMap<>();
                        for (Identifier id : modifiers) {
                            String[] parts = id.getPath().split("_");
                            if (parts.length > 0) {
                                prefixCount.merge(parts[0], 1, Integer::sum);
                            }
                        }

                        for (Identifier id : modifiers) {
                            String group = ReforgeUtil.getDynamicGroupName(id, prefixCount);
                            if (prefixCount.getOrDefault(group, 0) > 1) {
                                groupedModifiers.computeIfAbsent(group, k -> new ArrayList<>()).add(id);
                            } else {
                                ungroupedModifiers.add(id);
                            }
                        }
                    }
                }

            }

            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {

            }

            @Override
            protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                ItemStack stack = handler.getSlot(1).getStack();
                if (stack.isEmpty()) return;

                Identifier icon = Identifier.of("kevs", "textures/gui/anvil_sword.png");

                context.getMatrices().push();
                RenderSystem.setShaderColor(isHovered() ? 1f : 0.5f, isHovered() ? 1f : 0.5f, isHovered() ? 1f : 0.5f, 1f);
                context.drawTexture(icon, getX(), getY(), 0, 0, this.width, this.height, 16, 16);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                context.getMatrices().pop();
            }
        };

        this.addDrawableChild(showModifiersButton);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {


        int x = ConfigInit.CONFIG.leftSideModifierList
                ? this.x - 130
                : this.x + this.backgroundWidth + 10;

        int baseY = this.y + 10;
        int rendered = 0;
        if (!modifiersVisible || (groupedModifiers.isEmpty() && ungroupedModifiers.isEmpty())) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        for (Map.Entry<String, List<Identifier>> entry : groupedModifiers.entrySet()) {
                String group = entry.getKey();
                List<Identifier> modifiers = entry.getValue();
                int groupY = baseY - scrollOffset + rendered * entryHeight;

                if (mouseX >= x && mouseX <= x + 120 && mouseY >= groupY && mouseY <= groupY + entryHeight) {
                    if (modifiers.size() == 1) {
                        Identifier id = modifiers.get(0);
                        if (targetModifier != null && targetModifier.equals(id)) {
                            targetModifier = null;
                            modifierAchieved = false;
                        } else {
                            targetModifier = id;
                            modifierAchieved = false;
                        }
                    } else {
                        if (expandedGroups.contains(group)) {
                            expandedGroups.remove(group);
                        } else {
                            expandedGroups.add(group);
                        }
                    }
                    return true;
                }



                rendered++;

                if (expandedGroups.contains(group)) {
                    for (Identifier id : modifiers) {
                        int modY = baseY - scrollOffset + rendered * entryHeight;
                        if (mouseX >= x && mouseX <= x + 140 && mouseY >= modY && mouseY <= modY + entryHeight) {

                            if (targetModifier != null && targetModifier.equals(id)) {
                                targetModifier = null;
                                modifierAchieved = false;
                            }

                            else if (!modifierAchieved || !id.equals(targetModifier)) {
                                targetModifier = id;
                                modifierAchieved = false;
                            }
                            return true;
                        }
                        rendered++;
                    }
                }
        }
        int ungroupedY = this.y + 10 + rendered * entryHeight;
        for (Identifier id : ungroupedModifiers) {
            int modY = ungroupedY - scrollOffset;
            if (mouseX >= x && mouseX <= x + 140 && mouseY >= modY && mouseY <= modY + entryHeight) {
                if (targetModifier != null && targetModifier.equals(id) ) {
                    targetModifier = null;
                    modifierAchieved = false;
                } else {
                    targetModifier = id;
                    modifierAchieved = false;
                }
                return true;
            }
            ungroupedY += entryHeight;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }





    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!modifiersVisible) return false;

        int groupedCount = groupedModifiers.entrySet().stream()
                .mapToInt(entry -> 1 + (expandedGroups.contains(entry.getKey()) ? entry.getValue().size() : 0))
                .sum();
        int totalEntries = groupedCount + ungroupedModifiers.size();

        int maxOffset = Math.max(0, (totalEntries - maxVisibleEntries) * entryHeight);
        scrollOffset = Math.min(Math.max(scrollOffset - (int) (verticalAmount * entryHeight), 0), maxOffset);
        return true;
    }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        RenderSystem.disableBlend();
        drawMouseoverTooltip(context, mouseX, mouseY);

        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        if (this.isPointWithinBounds(79, 56, 18, 18, mouseX, mouseY)) {
            ItemStack itemStack = this.getScreenHandler().getSlot(1).getStack();
            if (itemStack == null || itemStack.isEmpty() || itemStack.isIn(TieredItemTags.MODIFIER_RESTRICTED)) {
                baseItems = Collections.emptyList();
            } else {
                if (itemStack != last) {
                    last = itemStack;
                    baseItems = new ArrayList<>();
                    List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(itemStack.getItem());
                    if (!items.isEmpty()) {
                        baseItems.addAll(items);
                    } else if (itemStack.getItem() instanceof ToolItem toolItem) {
                        for (ItemStack s : toolItem.getMaterial().getRepairIngredient().getMatchingStacks()) {
                            baseItems.add(s.getItem());
                        }
                    } else if (itemStack.getItem() instanceof ArmorItem armorItem && armorItem.getMaterial().value().repairIngredient() != null) {
                        for (ItemStack s : armorItem.getMaterial().value().repairIngredient().get().getMatchingStacks()) {
                            baseItems.add(s.getItem());
                        }
                    } else {
                        for (RegistryEntry<Item> itemRegistryEntry : Registries.ITEM.getOrCreateEntryList(TieredItemTags.REFORGE_BASE_ITEM)) {
                            baseItems.add(itemRegistryEntry.value());
                        }
                    }
                }
            }

            List<Text> tooltip = new ArrayList<>();
            if (!baseItems.isEmpty()) {
                ItemStack ingredient = this.getScreenHandler().getSlot(0).getStack();
                if (ingredient == null || ingredient.isEmpty() || !baseItems.contains(ingredient.getItem())) {
                    tooltip.add(Text.translatable("screen.tiered.reforge_ingredient"));
                    for (Item item : baseItems) {
                        tooltip.add(item.getName());
                    }
                }
            }

            if (itemStack.isDamageable() && itemStack.isDamaged()) {
                tooltip.add(Text.translatable("screen.tiered.reforge_damaged"));
            }

            if (!tooltip.isEmpty()) {
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }
        }

        if (!ConfigInit.CONFIG.uniqueReforge
                && !this.getScreenHandler().getSlot(1).getStack().isEmpty()
                && ModifierUtils.getAttributeId(this.getScreenHandler().getSlot(1).getStack()) != null
                && ModifierUtils.getAttributeId(this.getScreenHandler().getSlot(1).getStack()).getPath().contains("unique")) {
            context.drawTexture(TEXTURE, this.x + 74, this.y + 29, 0, 166, 28, 26);
        }


        if (modifiersVisible && (!groupedModifiers.isEmpty() || !ungroupedModifiers.isEmpty())) {
            boolean hoveredTooltipDrawn = false;

            int listX = ConfigInit.CONFIG.leftSideModifierList
                    ? this.x - 135
                    : this.x + this.backgroundWidth + 5;

            int listY = this.y + 10;
            int maxWidth = 130;


            int totalHeight = maxVisibleEntries * entryHeight;
            int top = listY - 4;
            int bottom = top + totalHeight + 8;
            context.fill(listX - 4, top, listX + maxWidth + 4, bottom, 0xBB111111);
            context.drawBorder(listX - 4, top, maxWidth + 8, bottom - top, 0xFF666666);


            List<Text> tooltipToDraw = null;
            int tooltipX = 0, tooltipY = 0;


            int scaleFactor = (int) this.client.getWindow().getScaleFactor();
            RenderSystem.enableScissor(
                    (listX - 4) * scaleFactor,
                    (this.height - (listY + maxVisibleEntries * entryHeight + 4)) * scaleFactor,
                    (maxWidth + 8) * scaleFactor,
                    (maxVisibleEntries * entryHeight + 8) * scaleFactor
            );

            int rendered = 0;
            for (Map.Entry<String, List<Identifier>> entry : groupedModifiers.entrySet()) {
                String rarity = entry.getKey();
                List<Identifier> modifiers = entry.getValue();
                int groupY = listY - scrollOffset + rendered * entryHeight;

                boolean groupHeaderVisible = !(groupY + entryHeight < listY || groupY > listY + (maxVisibleEntries * entryHeight));


                String displayName = capitalize(rarity);
                boolean isSingleEntry = modifiers.size() == 1 && groupedModifiers.get(rarity).size() == 1;
                String prefix = isSingleEntry ? "• " : (expandedGroups.contains(rarity) ? "▼ " : "▶ ");


                context.fill(listX - 2, groupY - 1, listX + maxWidth - 2, groupY + entryHeight, 0x55222222);
                context.drawText(this.textRenderer, Text.literal(prefix + displayName).styled(s -> s.withBold(true)), listX, groupY, 0xFFFFFF, false);

                if (!hoveredTooltipDrawn && mouseX >= listX && mouseX <= listX + 120 && mouseY >= groupY && mouseY <= groupY + entryHeight) {
                    tooltipToDraw = List.of(Text.literal("Click to expand/collapse"));
                    tooltipX = mouseX;
                    tooltipY = mouseY;
                    hoveredTooltipDrawn = true;
                }

                rendered++;

                if (modifiers.size() == 1 || expandedGroups.contains(rarity)) {
                    for (Identifier id : modifiers) {
                        int modY = listY - scrollOffset + rendered * entryHeight;

                        if (modY + entryHeight < listY || modY > listY + (maxVisibleEntries * entryHeight)) {
                            rendered++;
                            continue;
                        }

                        String niceName;
                        if (groupedModifiers.containsKey(rarity) && groupedModifiers.get(rarity).size() > 1) {
                            String groupPrefix = rarity.toLowerCase() + "_";
                            String fullName = id.getPath();
                            if (fullName.startsWith(groupPrefix)) {
                                String trimmed = fullName.substring(groupPrefix.length());
                                niceName = ReforgeUtil.formatModifierName(Identifier.of(id.getNamespace(), trimmed));
                            } else {
                                niceName = ReforgeUtil.formatModifierName(id);
                            }
                        } else {
                            niceName = ReforgeUtil.formatModifierName(id);
                        }

                        boolean isHovered = mouseX >= listX && mouseX <= listX + 140 && mouseY >= modY && mouseY <= modY + entryHeight;

                        if (isHovered && !hoveredTooltipDrawn) {
                            context.fill(listX - 2, modY - 1, listX + maxWidth - 2, modY + entryHeight, 0x44FFFFFF);
                        }

                        String label = "• " + niceName;
                        int modColor = ReforgeUtil.getColorForModifier(id);
                        int maxTextWidth = 120;
                        String trimmed = textRenderer.trimToWidth(label, maxTextWidth).toString();
                        int textX = listX + 5;
                        int textY = modY + 2;
                        context.drawText(textRenderer, Text.literal(trimmed), textX, textY, modColor, false);

                        if (id.equals(targetModifier)) {
                            Identifier lockIcon = Identifier.of("kevs", "textures/gui/lock.png");
                            int iconSize = 8;
                            int textWidth = textRenderer.getWidth(trimmed);
                            int iconX = textX + textWidth + 4; // 4px padding after text
                            int iconY = textY;

                            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                            context.drawTexture(lockIcon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                        }

                        if (isHovered && !hoveredTooltipDrawn) {
                            ItemStack base = handler.getSlot(1).getStack();
                            if (!base.isEmpty()) {
                                ItemStack preview = base.copy();
                                ModifierUtils.setItemStackAttributeWithId(preview, id);

                                Item.TooltipContext tooltipContext = new Item.TooltipContext() {
                                    @Nullable public RegistryWrapper.WrapperLookup getRegistryLookup() { return null; }
                                    public float getUpdateTickRate() { return 0; }
                                    @Nullable public MapState getMapState(MapIdComponent id) { return null; }
                                };

                                tooltipToDraw = preview.getTooltip(tooltipContext, client.player, TooltipType.ADVANCED);
                            } else {
                                tooltipToDraw = List.of(Text.literal("Modifier: " + id.getPath()));
                            }

                            tooltipX = mouseX;
                            tooltipY = mouseY;
                            hoveredTooltipDrawn = true;
                        }

                        rendered++;
                    }
                }

            }
            for (Identifier id : ungroupedModifiers) {
                int modY = listY - scrollOffset + rendered * entryHeight;

                if (modY + entryHeight < listY || modY > listY + (maxVisibleEntries * entryHeight)) {
                    rendered++;
                    continue;
                }

                String niceName = formatModifierName(id);
                boolean isHovered = mouseX >= listX && mouseX <= listX + 140 && mouseY >= modY && mouseY <= modY + entryHeight;

                if (isHovered && !hoveredTooltipDrawn) {
                    context.fill(listX - 2, modY - 1, listX + maxWidth - 2, modY + entryHeight, 0x44FFFFFF);
                }

                String label = "• " + niceName;
                int modColor = ReforgeUtil.getColorForModifier(id);
                int maxTextWidth = 120;
                String trimmed = textRenderer.trimToWidth(label, maxTextWidth).toString();
                int textX = listX + 5;
                int textY = modY + 2;
                context.drawText(textRenderer, Text.literal(trimmed), textX, textY, modColor, false);

                if (id.equals(targetModifier)) {
                    Identifier lockIcon = Identifier.of("kevs", "textures/gui/lock.png");
                    int iconSize = 8;
                    int textWidth = textRenderer.getWidth(trimmed);
                    int iconX = textX + textWidth + 4; // 4px padding after text
                    int iconY = textY;

                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                    context.drawTexture(lockIcon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                }

                if (isHovered && !hoveredTooltipDrawn) {
                    ItemStack base = handler.getSlot(1).getStack();
                    if (!base.isEmpty()) {
                        ItemStack preview = base.copy();
                        ModifierUtils.setItemStackAttributeWithId(preview, id);

                        Item.TooltipContext tooltipContext = new Item.TooltipContext() {
                            @Nullable public RegistryWrapper.WrapperLookup getRegistryLookup() { return null; }
                            public float getUpdateTickRate() { return 0; }
                            @Nullable public MapState getMapState(MapIdComponent id) { return null; }
                        };

                        tooltipToDraw = preview.getTooltip(tooltipContext, client.player, TooltipType.ADVANCED);
                    } else {
                        tooltipToDraw = List.of(Text.literal("Modifier: " + id.getPath()));
                    }

                    tooltipX = mouseX;
                    tooltipY = mouseY;
                    hoveredTooltipDrawn = true;
                }

                rendered++;
            }

            RenderSystem.disableScissor();

            if (tooltipToDraw != null) {
                context.drawTooltip(this.textRenderer, tooltipToDraw, tooltipX, tooltipY);
            }


            int groupedCount = groupedModifiers.entrySet().stream()
                    .mapToInt(entry -> 1 + (expandedGroups.contains(entry.getKey()) ? entry.getValue().size() : 0))
                    .sum();
            int totalEntries = groupedCount + ungroupedModifiers.size();
            int contentHeight = totalEntries * entryHeight;
            int viewHeight = maxVisibleEntries * entryHeight;

            if (contentHeight > viewHeight) {
                int scrollbarHeight = Math.max((int) ((float) viewHeight / contentHeight * viewHeight), 10);
                int scrollPosition = (int) ((float) scrollOffset / (contentHeight - viewHeight) * (viewHeight - scrollbarHeight));
                int scrollbarX = listX + maxWidth - 5;
                int scrollbarY = listY + scrollPosition;

                context.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0xFF888888);
            }

        }

        Iterator<FloatingText> iterator = floatingTexts.iterator();
        while (iterator.hasNext()) {
            FloatingText ft = iterator.next();
            ft.tick();

            if (ft.isExpired()) {
                iterator.remove();
                continue;
            }

            int color = ft.getRenderColor();
            float yOffset = ft.getYOffset();

            int textWidth = this.textRenderer.getWidth(ft.text);
            int drawX = ft.x - textWidth / 2;
            int drawY = (int)(ft.y + yOffset);

            context.drawText(this.textRenderer, ft.text, drawX, drawY, color, true);
        }
    }


    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void removed() {
        super.removed();
        handler.removeListener(this);
    }

    @Override public void onPropertyUpdate(ScreenHandler handler, int property, int value) {}
    @Override
    public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
        if (slotId == 1) {

            if (!ItemStack.areItemsEqual(stack, lastSeenStack)) {
                lastSeenStack = stack.copy();

                if (modifiersVisible) {
                    groupedModifiers.clear();
                    expandedGroups.clear();
                    ungroupedModifiers.clear();
                    scrollOffset = 0;

                    if (!stack.isEmpty()) {
                        List<Identifier> modifiers = ReforgeUtil.getAvailableModifiers(stack);
                        modifiers.sort(Comparator
                                .comparing(ReforgeUtil::getRarityOrder)
                                .thenComparing(ReforgeUtil::getNumericSuffixOrZero)
                        );

                        Map<String, Integer> prefixCount = new HashMap<>();
                        for (Identifier id : modifiers) {
                            String[] parts = id.getPath().split("_");
                            if (parts.length > 0) {
                                prefixCount.merge(parts[0], 1, Integer::sum);
                            }
                        }

                        for (Identifier id : modifiers) {
                            String group = ReforgeUtil.getDynamicGroupName(id, prefixCount);
                            if (prefixCount.getOrDefault(group, 0) > 1) {
                                groupedModifiers.computeIfAbsent(group, k -> new ArrayList<>()).add(id);
                            } else {
                                ungroupedModifiers.add(id);
                            }
                        }
                    }
                }
            }


            if (!stack.isEmpty()) {
                Identifier newId = ModifierUtils.getAttributeId(stack);
                if (newId != null && (!newId.equals(lastSeenModifier) || !ItemStack.areItemsEqual(stack, lastSeenStack))) {
                    lastSeenModifier = newId;

                    int slotX = this.getScreenHandler().getSlot(1).x;
                    int slotY = this.getScreenHandler().getSlot(1).y;
                    int x = this.x + slotX + 8;
                    int y = this.y + slotY - 6;

                    if (targetModifier != null && newId.equals(targetModifier)) {
                        modifierAchieved = true;
                        floatingTexts.add(new FloatingText(Text.literal("✔ Modifier Achieved"), 0x55FF55, x, y));
                        if (reforgeButton != null) {
                            reforgeButton.setDisabled(true);
                        }
                        return;
                    }

                    Map<String, Integer> prefixFrequency = new HashMap<>();
                    String[] parts = newId.getPath().toLowerCase().split("_");
                    if (parts.length > 0) {
                        prefixFrequency.put(parts[0], 2);
                    }

                    String group = ReforgeUtil.getDynamicGroupName(newId, prefixFrequency);
                    Text displayText = Text.literal("✦ " + capitalize(group) + " ✦");
                    int color = ReforgeUtil.getColorForModifier(newId);
                    floatingTexts.add(new FloatingText(displayText, color, x, y));

                    if (reforgeButton != null && reforgeButton.disabled && !modifierAchieved) {
                        reforgeButton.setDisabled(false);
                    }
                }
            }
        }
    }







    @Override public Class<?> getParentScreenClass() { return AnvilScreen.class; }

    public class ReforgeButton extends ButtonWidget {
        private boolean disabled;

        public ReforgeButton(int x, int y, PressAction onPress) {
            super(x, y, 18, 18, ScreenTexts.EMPTY, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.disabled = true;
            this.active = true;
        }
        public void press() {
            this.onPress.onPress(this);
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            int j = 176;
            if (this.disabled) {
                j += this.width * 2;
            } else if (this.isHovered()) {
                j += this.width;
            }
            context.drawTexture(TEXTURE, this.getX(), this.getY(), j, 0, this.width, this.height);
        }
        public void setDisabled(boolean disable) {
            this.disabled = disable;

            this.active = true;
        }
    }



}
