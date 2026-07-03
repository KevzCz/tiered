package draylar.tiered.reforge;

import com.mojang.blaze3d.systems.RenderSystem;
import draylar.tiered.Tiered;
import draylar.tiered.TieredKeybinds;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.api.TieredItemTags;
import draylar.tiered.api.effect.DataEffect;
import draylar.tiered.api.effect.ReforgeEffects;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.api.imprint.Imprints;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.network.TieredClientPacket;
import draylar.tiered.reforge.codex.CodexScreen;
import draylar.tiered.registry.ModComponents;
import draylar.tiered.util.ImprintPlatesComponent;
import draylar.tiered.util.ImprintSlots;
import draylar.tiered.util.ReforgeMaterialTooltip;
import draylar.tiered.util.ReforgeMaterials;
import draylar.tiered.util.ReforgeUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import draylar.tiered.lib.Tab;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static draylar.tiered.util.ReforgeUtil.formatModifierName;

@Environment(EnvType.CLIENT)
public class ReforgeScreen extends HandledScreen<ReforgeScreenHandler> implements ScreenHandlerListener, Tab {
    @Nullable
    private Identifier targetModifier = null;
    private final Set<String> targetModifierGroups = new HashSet<>();
    private final Set<Identifier> targetGroupModifiers = new HashSet<>();
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
    private ClickableWidget autoReforgeToggle;
    private ClickableWidget autoRefillToggle;
    private ClickableWidget infoButton;
    private ClickableWidget codexButton;

    private static final float GLYPH_NUDGE_X = 0.5f;
    private static final float GLYPH_NUDGE_Y = -0.5f;

    private boolean autoReforging = false;
    private boolean autoRefillEnabled = false;
    private int autoReforgeCooldown = 0;
    private static final int AUTO_REFORGE_DELAY_MIN = 3;
    private static final int AUTO_REFORGE_DELAY_MAX = 10;
    private int materialLowTicks = 0;
    private static final int MATERIAL_LOW_DEBOUNCE = 10;

    private ItemStack last;
    private List<Item> baseItems;

    private int previewCycleTick = 0;
    private static final int PREVIEW_CYCLE_INTERVAL = 20;
    private int previewPanelX = Integer.MIN_VALUE;
    private int previewPanelY = Integer.MIN_VALUE;
    private boolean draggingPreview = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private static boolean suppressNoSlotConfirm = false;

    private static final float PREVIEW_SCALE_MIN = 0.5f;
    private static final float PREVIEW_SCALE_MAX = 2.5f;

    private float previewScale = PREVIEW_SCALE_MIN;
    private static final int PREVIEW_RESIZE_GRIP = 8;
    private boolean resizingPreview = false;
    private int resizeStartMouseX = 0;
    private int resizeStartMouseY = 0;
    private float resizeStartScale = 1.0f;
    private int resizeBaseW = 0;
    private int resizeBaseH = 0;

    private boolean modifiersVisible = false;
    private final Map<String, List<Identifier>> groupedModifiers = new LinkedHashMap<>();
    private final Set<String> expandedGroups = new HashSet<>();
    private int scrollOffset = 0;
    private final int entryHeight = 12;
    private final int maxVisibleEntries = 12;
    private final List<Identifier> ungroupedModifiers = new ArrayList<>();

    @Nullable private Set<Identifier> producibleCache = null;
    @Nullable private Item producibleCacheTarget = null;
    @Nullable private Item producibleCacheAddition = null;
    private boolean producibleCacheValid = false;

    private Map<Identifier, Float> attributeOddsCache = Map.of();
    @Nullable private Item oddsCacheTarget = null;
    @Nullable private Item oddsCacheAddition = null;
    private boolean oddsCacheValid = false;

    private float slotPulseAnimation = 0f;
    private float slotScaleAnimation = 1f;
    private int slotGlowColor = 0xFFFFFF;

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

                ItemStack target = handler.getSlot(1).getStack();
                int extractChance = extractChancePercent(handler.getSlot(2).getStack());
                if (extractChance >= 0 && Imprints.slotsUsed(target) > 0) {
                    if (this.client != null) this.client.setScreen(new ExtractChooserScreen(this, target, extractChance));
                } else if (noFreeImprintSlot() && !suppressNoSlotConfirm) {

                    openNoSlotConfirm();
                } else {
                    TieredClientPacket.writeC2SReforgePacket();
                }
            }
        }));

        int toggleX = x + 134;
        int toggleY = y + 30;
        int toggleSize = 10;
        int toggleSpacing = 14;

        this.autoRefillToggle = new ClickableWidget(toggleX, toggleY, toggleSize, toggleSize, Text.empty()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                autoRefillEnabled = !autoRefillEnabled;
                TieredClientPacket.writeC2SAutoRefillPacket(autoRefillEnabled);
            }

            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                return super.isMouseOver(mouseX, mouseY) && !isOverPreviewPanel(mouseX, mouseY);
            }

            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

            @Override
            protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                int bgColor = autoRefillEnabled ? 0xFF44AA44 : 0xFF444444;
                int borderColor = isHovered() ? 0xFFFFFFFF : 0xFF888888;
                context.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
                context.drawBorder(getX(), getY(), width, height, borderColor);
                if (autoRefillEnabled) {
                    context.drawText(textRenderer, Text.literal("✓"), getX() + 2, getY() + 1, 0xFFFFFFFF, false);
                }
            }
        };
        this.addDrawableChild(autoRefillToggle);

        this.autoReforgeToggle = new ClickableWidget(toggleX, toggleY + toggleSpacing, toggleSize, toggleSize, Text.empty()) {
            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                return super.isMouseOver(mouseX, mouseY) && !isOverPreviewPanel(mouseX, mouseY);
            }

            @Override
            public void onClick(double mouseX, double mouseY) {
                if (autoReforging) {
                    stopAutoReforge();
                } else if (hasAnyTarget() && !modifierAchieved && !reforgeButton.disabled) {
                    startAutoReforge();
                }
            }

            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

            @Override
            protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                boolean canAutoReforge = hasAnyTarget() && !modifierAchieved && !reforgeButton.disabled;
                int bgColor = autoReforging ? 0xFF4444AA : (canAutoReforge ? 0xFF444444 : 0xFF222222);
                int borderColor = isHovered() && canAutoReforge ? 0xFFFFFFFF : 0xFF888888;
                context.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
                context.drawBorder(getX(), getY(), width, height, borderColor);
                if (autoReforging) {
                    context.drawText(textRenderer, Text.literal("▶"), getX() + 2, getY() + 1, 0xFFFFFFFF, false);
                }
            }
        };
        this.addDrawableChild(autoReforgeToggle);

        this.infoButton = new ClickableWidget(toggleX, toggleY + toggleSpacing * 2, toggleSize, toggleSize, Text.empty()) {
            @Override
            public void onClick(double mouseX, double mouseY) {}

            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                return super.isMouseOver(mouseX, mouseY) && !isOverPreviewPanel(mouseX, mouseY);
            }

            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

            @Override
            protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                int bgColor = 0xFF555555;
                int borderColor = isHovered() ? 0xFFFFFFFF : 0xFF888888;
                context.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
                context.drawBorder(getX(), getY(), width, height, borderColor);
                drawCenteredGlyph(context, "?", 0xFFFFFFFF);
            }

            private void drawCenteredGlyph(DrawContext context, String glyph, int color) {
                int gw = textRenderer.getWidth(glyph);
                int gx = getX() + (width - gw + 1) / 2;
                int gy = getY() + (height - textRenderer.fontHeight + 1) / 2 + 1;

                context.getMatrices().push();
                context.getMatrices().translate(GLYPH_NUDGE_X, GLYPH_NUDGE_Y, 0f);
                context.drawText(textRenderer, Text.literal(glyph), gx, gy, color, false);
                context.getMatrices().pop();
            }
        };
        this.addDrawableChild(infoButton);

        int codexSize = 12;
        int codexX = x + this.backgroundWidth - codexSize - 4;
        int codexY = y + 4;
        this.codexButton = new ClickableWidget(codexX, codexY, codexSize, codexSize,
                Text.translatable("screen.tiered.codex.title")) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                if (client != null) client.setScreen(new CodexScreen(ReforgeScreen.this));
            }

            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                return super.isMouseOver(mouseX, mouseY) && !isOverPreviewPanel(mouseX, mouseY);
            }

            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

            @Override
            protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                int bgColor = isHovered() ? 0xFF5A3A82 : 0xFF4A2E6E;
                int borderColor = isHovered() ? 0xFFB088E0 : 0xFF8A6AB0;
                context.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
                context.drawBorder(getX(), getY(), width, height, borderColor);
                String glyph = "✦";
                int gw = textRenderer.getWidth(glyph);
                int gx = getX() + (width - gw + 1) / 2;
                int gy = getY() + (height - textRenderer.fontHeight + 1) / 2 + 1;
                context.getMatrices().push();
                context.getMatrices().translate(GLYPH_NUDGE_X, GLYPH_NUDGE_Y, 0f);
                context.drawText(textRenderer, Text.literal(glyph), gx, gy,
                        isHovered() ? 0xFFFFFFFF : 0xFFE0C8FF, false);
                context.getMatrices().pop();
                if (isHovered()) {
                    setTooltip(Tooltip.of(
                            Text.translatable("screen.tiered.codex.title")));
                }
            }
        };
        this.addDrawableChild(codexButton);

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

    private void startAutoReforge() {
        autoReforging = true;
        autoReforgeCooldown = 0;
    }

    private void stopAutoReforge() {
        autoReforging = false;
        autoReforgeCooldown = 0;
    }

    public void stopAutoReforgeFromServer() {
        autoReforging = false;
        autoReforgeCooldown = 0;
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();

        previewCycleTick++;

        if (slotPulseAnimation > 0) {
            slotPulseAnimation -= 0.05f;
            if (slotPulseAnimation < 0) slotPulseAnimation = 0;
        }
        if (slotScaleAnimation > 1f) {
            slotScaleAnimation -= 0.02f;
            if (slotScaleAnimation < 1f) slotScaleAnimation = 1f;
        }

        if (autoReforging) {
            if (modifierAchieved) {
                materialLowTicks = 0;
                stopAutoReforge();
                return;
            }

            if (reforgeButton.disabled) {
                materialLowTicks++;
                if (materialLowTicks >= MATERIAL_LOW_DEBOUNCE) {
                    materialLowTicks = 0;
                    stopAutoReforge();
                }
                return;
            } else {
                materialLowTicks = 0;
            }

            if (autoReforgeCooldown > 0) {
                autoReforgeCooldown--;
                return;
            }

            TieredClientPacket.writeC2SReforgePacket();
            autoReforgeCooldown = calculateAdaptiveDelay();
        }
    }

    private int calculateAdaptiveDelay() {
        ReforgeScreenHandler handler = this.getScreenHandler();
        ItemStack base = handler.getSlot(0).getStack();
        ItemStack addition = handler.getSlot(2).getStack();
        int minCount = Math.min(
            base.isEmpty() ? 0 : base.getCount(),
            addition.isEmpty() ? 0 : addition.getCount()
        );
        if (minCount >= 32) return AUTO_REFORGE_DELAY_MIN;
        if (minCount >= 16) return AUTO_REFORGE_DELAY_MIN + 1;
        if (minCount >= 8) return AUTO_REFORGE_DELAY_MIN + 2;
        if (minCount >= 4) return AUTO_REFORGE_DELAY_MAX - 2;
        return AUTO_REFORGE_DELAY_MAX;
    }

    private void triggerReforgeEffect(int color, boolean isAchieved) {
        slotPulseAnimation = 1f;
        slotScaleAnimation = isAchieved ? 1.3f : 1.15f;
        slotGlowColor = color;

        if (this.client != null && this.client.player != null) {
            if (isAchieved) {
                this.client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            } else {
                this.client.player.playSound(SoundEvents.BLOCK_ANVIL_USE, 0.5f, 1.0f + (float)(Math.random() * 0.2 - 0.1));
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (autoReforging && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            stopAutoReforge();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (button == 0) {
            int[] bounds = previewPanelBounds();
            if (bounds != null && mouseX >= bounds[0] && mouseX <= bounds[0] + bounds[2]
                    && mouseY >= bounds[1] && mouseY <= bounds[1] + bounds[3]) {
                if (isOverResizeGrip(mouseX, mouseY, bounds)) {
                    int[] size = previewPanelContentSize();
                    if (size != null) {
                        resizingPreview = true;
                        resizeStartMouseX = (int) mouseX;
                        resizeStartMouseY = (int) mouseY;
                        resizeStartScale = previewScale;
                        resizeBaseW = size[0];
                        resizeBaseH = size[1];
                    }
                    return true;
                }
                draggingPreview = true;
                dragOffsetX = (int) mouseX - previewPanelX;
                dragOffsetY = (int) mouseY - previewPanelY;
                return true;
            }
        }

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
                    if (button == 1) {
                        if (targetModifierGroups.contains(group)) {
                            targetModifierGroups.remove(group);
                            rebuildTargetGroupModifiers();
                            if (targetModifierGroups.isEmpty()) {
                                targetModifier = null;
                            }
                            modifierAchieved = false;
                            stopAutoReforge();
                        } else {
                            targetModifierGroups.add(group);
                            targetGroupModifiers.addAll(modifiers);
                            targetModifier = null;
                            modifierAchieved = false;
                            stopAutoReforge();
                        }
                        return true;
                    }

                    if (modifiers.size() == 1) {
                        Identifier id = modifiers.get(0);
                        if (targetModifier != null && targetModifier.equals(id)) {
                            clearTarget();
                        } else {
                            setTargetModifier(id);
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
                                clearTarget();
                            }

                            else if (!modifierAchieved || !id.equals(targetModifier)) {
                                setTargetModifier(id);
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
                    clearTarget();
                } else {
                    setTargetModifier(id);
                }
                return true;
            }
            ungroupedY += entryHeight;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (resizingPreview && button == 0) {

            float ratioX = resizeBaseW <= 0 ? 1f : (resizeBaseW * resizeStartScale + (float) (mouseX - resizeStartMouseX)) / (resizeBaseW * resizeStartScale);
            float ratioY = resizeBaseH <= 0 ? 1f : (resizeBaseH * resizeStartScale + (float) (mouseY - resizeStartMouseY)) / (resizeBaseH * resizeStartScale);
            float scale = resizeStartScale * (ratioX + ratioY) / 2f;
            previewScale = Math.max(PREVIEW_SCALE_MIN, Math.min(PREVIEW_SCALE_MAX, scale));
            return true;
        }
        if (draggingPreview && button == 0) {
            previewPanelX = (int) mouseX - dragOffsetX;
            previewPanelY = (int) mouseY - dragOffsetY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (draggingPreview || resizingPreview)) {
            draggingPreview = false;
            resizingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void clearTarget() {
        targetModifier = null;
        targetModifierGroups.clear();
        targetGroupModifiers.clear();
        modifierAchieved = false;
        stopAutoReforge();
    }

    private void setTargetModifier(Identifier id) {
        targetModifier = id;
        targetModifierGroups.clear();
        targetGroupModifiers.clear();
        modifierAchieved = false;
        stopAutoReforge();
    }

    private void rebuildTargetGroupModifiers() {
        targetGroupModifiers.clear();
        for (String group : targetModifierGroups) {
            List<Identifier> modifiers = groupedModifiers.get(group);
            if (modifiers != null) {
                targetGroupModifiers.addAll(modifiers);
            }
        }
    }

    private boolean isTargetAchieved(Identifier rolledModifier) {
        if (targetModifier != null) {
            return rolledModifier.equals(targetModifier);
        }
        if (!targetModifierGroups.isEmpty() && !targetGroupModifiers.isEmpty()) {
            return targetGroupModifiers.contains(rolledModifier);
        }
        return false;
    }

    private boolean hasAnyTarget() {
        return targetModifier != null || (!targetModifierGroups.isEmpty() && !targetGroupModifiers.isEmpty());
    }

    @Nullable
    private String findAchievedGroup(Identifier rolledModifier) {
        for (String group : targetModifierGroups) {
            List<Identifier> modifiers = groupedModifiers.get(group);
            if (modifiers != null && modifiers.contains(rolledModifier)) {
                return group;
            }
        }
        return null;
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

    private static String formatOddsPercent(float fraction) {
        float pct = fraction * 100f;
        if (pct > 0f && pct < 0.1f) return "<0.1";
        return String.format(Locale.ROOT, "%.1f", pct);
    }

    private ItemStack buildPreviewStack() {
        ItemStack target = handler.getSlot(1).getStack();
        if (target == null || target.isEmpty()) return null;
        if (target.isIn(TieredItemTags.MODIFIER_RESTRICTED)) return null;

        ItemStack addition = handler.getSlot(2).getStack();
        ReforgeMaterial material = (addition == null || addition.isEmpty())
                ? null : ReforgeMaterials.resolve(addition);

        List<Identifier> producible = ModifierUtils.getProducibleAttributes(target.getItem(), material);
        if (producible.isEmpty()) return null;

        ItemStack preview = target.copy();
        int idx = (previewCycleTick / PREVIEW_CYCLE_INTERVAL) % producible.size();
        ModifierUtils.setItemStackAttributeWithId(preview, producible.get(idx));
        return preview;
    }

    @Nullable
    private Set<Identifier> computeProducibleSet() {
        Item targetItem = handler.getSlot(1).getStack().getItem();
        Item additionItem = handler.getSlot(2).getStack().getItem();

        if (producibleCacheValid && targetItem == producibleCacheTarget && additionItem == producibleCacheAddition) {
            return producibleCache;
        }

        producibleCacheTarget = targetItem;
        producibleCacheAddition = additionItem;
        producibleCacheValid = true;
        producibleCache = computeProducibleSetUncached();
        return producibleCache;
    }

    private Map<Identifier, Float> computeAttributeOdds() {
        Item targetItem = handler.getSlot(1).getStack().getItem();
        Item additionItem = handler.getSlot(2).getStack().getItem();

        if (oddsCacheValid && targetItem == oddsCacheTarget && additionItem == oddsCacheAddition) {
            return attributeOddsCache;
        }

        oddsCacheTarget = targetItem;
        oddsCacheAddition = additionItem;
        oddsCacheValid = true;
        attributeOddsCache = computeAttributeOddsUncached();
        return attributeOddsCache;
    }

    private Map<Identifier, Float> computeAttributeOddsUncached() {
        ItemStack target = handler.getSlot(1).getStack();
        if (target == null || target.isEmpty()) return Map.of();

        ItemStack addition = handler.getSlot(2).getStack();
        ReforgeMaterial material = (addition == null || addition.isEmpty())
                ? null : ReforgeMaterials.resolve(addition);

        return ModifierUtils.previewAttributeOdds(this.client != null ? this.client.player : null,
                target.getItem(), true, material);
    }

    private int extractChancePercent(ItemStack addition) {
        if (addition == null || addition.isEmpty()) return -1;
        ReforgeMaterial mat = ReforgeMaterials.resolve(addition);
        if (mat == null) return -1;
        DataEffect extract = ReforgeEffects.findExtract(mat.getEffects());
        if (extract == null) return -1;
        return Math.round(extract.definition().getValue() * 100);
    }

    private void openNoSlotConfirm() {
        if (this.client == null) return;
        this.client.setScreen(new NoSlotConfirmScreen(this,
                () -> TieredClientPacket.writeC2SReforgePacket(),
                () -> { suppressNoSlotConfirm = true; TieredClientPacket.writeC2SReforgePacket(); }));
    }

    private boolean noFreeImprintSlot() {
        ItemStack target = handler.getSlot(1).getStack();
        ItemStack addition = handler.getSlot(2).getStack();
        if (target.isEmpty() || addition.isEmpty()) return false;
        var content = addition.get(ModComponents.RUNE_CONTENT);
        boolean wouldGrant = content != null && !content.isEmpty();
        if (!wouldGrant) return false;
        return !Imprints.hasFreeSlot(target);
    }

    @Nullable
    private Set<Identifier> computeProducibleSetUncached() {
        ItemStack target = handler.getSlot(1).getStack();
        if (target == null || target.isEmpty()) return null;

        ItemStack addition = handler.getSlot(2).getStack();
        ReforgeMaterial material = (addition == null || addition.isEmpty())
                ? null : ReforgeMaterials.resolve(addition);

        if (material == null) return null;

        return new HashSet<>(ModifierUtils.getProducibleAttributes(target.getItem(), material));
    }

    private List<Text> buildPreviewTooltip(ItemStack preview) {
        Item.TooltipContext ctx = new Item.TooltipContext() {
            @Nullable public RegistryWrapper.WrapperLookup getRegistryLookup() { return null; }
            public float getUpdateTickRate() { return 0; }
            @Nullable public MapState getMapState(MapIdComponent id) { return null; }
        };
        List<Text> lines = new ArrayList<>(preview.getTooltip(ctx, client.player, TooltipType.BASIC));
        lines.add(0, Text.translatable("screen.tiered.reforge.preview.header").styled(s -> s.withColor(Formatting.DARK_GRAY).withItalic(true)));

        var imprints = preview.get(ModComponents.IMPRINTS);
        int cap = ImprintSlots.capacity(preview);
        if (cap > 0 || (imprints != null && !imprints.entries().isEmpty())) {
            int used = imprints == null ? 0 : imprints.slotsUsed();
            lines.add(Text.empty());
            lines.add(Text.translatable("screen.tiered.imprints.header.slots", used, cap)
                    .styled(s -> s.withColor(Formatting.GRAY)));
            if (imprints != null) {
                for (var entry : imprints.entries()) {
                    if (ImprintRegistry.get(entry.id()) == null) continue;
                    lines.add(ReforgeMaterialTooltip.imprintMarker(entry.id(), entry.value(), entry.extraValues()));
                }
            }
        }

        ItemStack addition = handler.getSlot(2).getStack();
        var content = addition.get(ModComponents.RUNE_CONTENT);
        if (content != null && !content.isEmpty()) {
            lines.add(Text.empty());
            lines.add(Text.translatable("screen.tiered.rune.grants.header").styled(s -> s.withColor(Formatting.GRAY)));
            for (var entry : content.entries()) {
                if (ImprintRegistry.get(entry.imprintId()) == null) continue;
                lines.add(ReforgeMaterialTooltip.imprintMarker(entry.imprintId(), entry.value(), entry.extraValues()));
            }
        }

        while (!lines.isEmpty() && lines.get(lines.size() - 1).getString().isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    private static final int PANEL_PAD = 6;
    private static final int PANEL_LINE_H = 11;
    private static final int PANEL_HANDLE_H = 9;

    private static final int MAX_PREVIEW_PLATE_W = 200;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        RenderSystem.disableBlend();

        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        if (slotPulseAnimation > 0) {
            int slotX = this.getScreenHandler().getSlot(1).x;
            int slotY = this.getScreenHandler().getSlot(1).y;
            int centerX = i + slotX + 8;
            int centerY = j + slotY + 8;

            float pulse = (float) Math.sin(slotPulseAnimation * Math.PI) * 0.5f + 0.5f;
            int alpha = (int) (pulse * 180);
            int glowSize = (int) (8 + (1 - slotPulseAnimation) * 12);

            int r = (slotGlowColor >> 16) & 0xFF;
            int g = (slotGlowColor >> 8) & 0xFF;
            int b = slotGlowColor & 0xFF;
            int color = (alpha << 24) | (r << 16) | (g << 8) | b;

            for (int ring = 0; ring < 3; ring++) {
                int size = glowSize + ring * 4;
                int ringAlpha = alpha / (ring + 1);
                int ringColor = (ringAlpha << 24) | (r << 16) | (g << 8) | b;
                context.fill(centerX - size, centerY - size, centerX + size, centerY + size, ringColor);
            }

            context.drawBorder(centerX - glowSize, centerY - glowSize, glowSize * 2, glowSize * 2, color | 0xFF000000);
        }

        renderPreviewPanel(context);

        drawMouseoverTooltip(context, mouseX, mouseY);

        int toggleX = i + 134;
        int toggleY = j + 30;
        int toggleSpacing = 14;
        context.drawText(this.textRenderer, Text.translatable("screen.tiered.reforge.label.refill"), toggleX + 12, toggleY + 1, 0xAAAAAA, false);
        context.drawText(this.textRenderer, Text.translatable("screen.tiered.reforge.label.auto"), toggleX + 12, toggleY + toggleSpacing + 1, 0xAAAAAA, false);
        context.drawText(this.textRenderer, Text.translatable("screen.tiered.reforge.label.info"), toggleX + 12, toggleY + toggleSpacing * 2 + 1, 0xAAAAAA, false);

        if (autoRefillToggle != null && this.isPointWithinBounds(134 - this.x + i, 30 - this.y + j, 50, 10, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer, Text.translatable("screen.tiered.reforge.tooltip.auto_refill"), mouseX, mouseY);
        }
        if (autoReforgeToggle != null && this.isPointWithinBounds(134 - this.x + i, 44 - this.y + j, 50, 10, mouseX, mouseY)) {
            List<Text> tooltip = new ArrayList<>();
            if (!hasAnyTarget()) {
                tooltip.add(Text.translatable("screen.tiered.reforge.tooltip.select_target"));
            } else if (autoReforging) {
                tooltip.add(Text.translatable("screen.tiered.reforge.tooltip.stop_auto"));
            } else {
                if (targetModifier != null) {
                    String modName = formatModifierName(targetModifier);
                    tooltip.add(Text.translatable("screen.tiered.reforge.tooltip.auto_until_modifier", modName));
                } else if (!targetModifierGroups.isEmpty()) {
                    String groupNames = String.join(", ", targetModifierGroups.stream().map(g -> capitalize(g)).toList());
                    tooltip.add(Text.translatable("screen.tiered.reforge.tooltip.auto_until_groups", groupNames));
                }
            }
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
        if (infoButton != null && this.isPointWithinBounds(134 - this.x + i, 58 - this.y + j, 50, 10, mouseX, mouseY)) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.translatable("screen.tiered.reforge.info.title").styled(s -> s.withBold(true).withColor(Formatting.GOLD)));
            tooltip.add(Text.empty());
            tooltip.add(Text.translatable("screen.tiered.reforge.info.specific").styled(s -> s.withColor(Formatting.WHITE)));
            tooltip.add(Text.translatable("screen.tiered.reforge.info.specific_example").styled(s -> s.withColor(Formatting.GRAY))
                .append(Text.literal("Legendary > Sharp").styled(s -> s.withColor(Formatting.GOLD))));
            tooltip.add(Text.empty());
            tooltip.add(Text.translatable("screen.tiered.reforge.info.groups").styled(s -> s.withColor(Formatting.WHITE)));
            tooltip.add(Text.translatable("screen.tiered.reforge.info.groups_example").styled(s -> s.withColor(Formatting.GRAY))
                .append(Text.literal("Legendary").styled(s -> s.withColor(Formatting.GOLD)))
                .append(Text.literal(" + ").styled(s -> s.withColor(Formatting.GRAY)))
                .append(Text.literal("Mythic").styled(s -> s.withColor(Formatting.LIGHT_PURPLE))));
            tooltip.add(Text.empty());
            tooltip.add(Text.translatable("screen.tiered.reforge.info.stops").styled(s -> s.withColor(Formatting.GREEN)));
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }

        if (this.isPointWithinBounds(79, 56, 18, 18, mouseX, mouseY)) {
            ItemStack itemStack = this.getScreenHandler().getSlot(1).getStack();
            ItemStack baseSlot = this.getScreenHandler().getSlot(0).getStack();
            ItemStack additionSlot = this.getScreenHandler().getSlot(2).getStack();

            List<Text> tooltip = new ArrayList<>();

            ReforgeMaterial additionMaterial = (additionSlot == null || additionSlot.isEmpty())
                    ? null : ReforgeMaterials.resolve(additionSlot);
            boolean noReforgeAddition = additionMaterial != null && additionMaterial.isSkipsBaseItem();

            if (itemStack == null || itemStack.isEmpty()) {
                tooltip.add(Text.literal("Place an item to reforge").styled(s -> s.withColor(Formatting.GRAY)));
            } else if (itemStack.isIn(TieredItemTags.MODIFIER_RESTRICTED)) {
                tooltip.add(Text.literal("This item cannot be reforged").styled(s -> s.withColor(Formatting.RED)));
            } else if (noReforgeAddition) {

                if (!additionSlot.isIn(TieredItemTags.REFORGE_ADDITION)) {
                    tooltip.add(Text.literal("Reforge Addition:").styled(s -> s.withColor(Formatting.AQUA)));
                    for (RegistryEntry<Item> itemEntry : Registries.ITEM.getOrCreateEntryList(TieredItemTags.REFORGE_ADDITION)) {
                        tooltip.add(Text.literal("  ").append(itemEntry.value().getName()).styled(s -> s.withColor(Formatting.DARK_AQUA)));
                    }
                }
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

                boolean needsBase = baseSlot == null || baseSlot.isEmpty() || (baseItems != null && !baseItems.isEmpty() && !baseItems.contains(baseSlot.getItem()));
                boolean needsAddition = additionSlot == null || additionSlot.isEmpty() || !additionSlot.isIn(TieredItemTags.REFORGE_ADDITION);

                if (needsBase && baseItems != null && !baseItems.isEmpty()) {
                    tooltip.add(Text.literal("Base Material:").styled(s -> s.withColor(Formatting.GOLD)));
                    for (Item item : baseItems) {
                        tooltip.add(Text.literal("  ").append(item.getName()).styled(s -> s.withColor(Formatting.YELLOW)));
                    }
                }

                if (needsAddition) {
                    if (!tooltip.isEmpty()) tooltip.add(Text.empty());
                    tooltip.add(Text.literal("Reforge Addition:").styled(s -> s.withColor(Formatting.AQUA)));
                    for (RegistryEntry<Item> itemEntry : Registries.ITEM.getOrCreateEntryList(TieredItemTags.REFORGE_ADDITION)) {
                        tooltip.add(Text.literal("  ").append(itemEntry.value().getName()).styled(s -> s.withColor(Formatting.DARK_AQUA)));
                    }
                }

                if (itemStack.isDamageable() && itemStack.isDamaged()) {
                    if (!tooltip.isEmpty()) tooltip.add(Text.empty());
                    tooltip.add(Text.translatable("screen.tiered.reforge_damaged").styled(s -> s.withColor(Formatting.RED)));
                }
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

            Set<Identifier> producible = computeProducibleSet();

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

            Map<Identifier, Float> attributeOdds = computeAttributeOdds();

            int rendered = 0;
            for (Map.Entry<String, List<Identifier>> entry : groupedModifiers.entrySet()) {
                String rarity = entry.getKey();
                List<Identifier> modifiers = entry.getValue();
                int groupY = listY - scrollOffset + rendered * entryHeight;

                boolean groupHeaderVisible = !(groupY + entryHeight < listY || groupY > listY + (maxVisibleEntries * entryHeight));

                String displayName = capitalize(rarity);
                boolean isSingleEntry = modifiers.size() == 1 && groupedModifiers.get(rarity).size() == 1;
                String prefix = isSingleEntry ? "• " : (expandedGroups.contains(rarity) ? "▼ " : "▶ ");

                if (!attributeOdds.isEmpty()) {
                    float groupOdds = 0f;
                    for (Identifier id : modifiers) groupOdds += attributeOdds.getOrDefault(id, 0f);
                    displayName += " (" + formatOddsPercent(groupOdds) + "%)";
                }

                boolean groupBlocked = producible != null && modifiers.stream().noneMatch(producible::contains);

                context.fill(listX - 2, groupY - 1, listX + maxWidth - 2, groupY + entryHeight, 0x55222222);
                Text groupText = Text.literal(prefix + displayName).styled(s -> s.withBold(true));
                context.drawText(this.textRenderer, groupText, listX, groupY, groupBlocked ? 0x777777 : 0xFFFFFF, false);

                if (targetModifierGroups.contains(rarity)) {
                    Identifier lockIcon = Identifier.of("kevs", "textures/gui/lock.png");
                    int iconSize = 8;
                    int textWidth = textRenderer.getWidth(groupText);
                    int iconX = listX + textWidth + 4;
                    int iconY = groupY;
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                    context.drawTexture(lockIcon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                }

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

                        boolean blocked = producible != null && !producible.contains(id);
                        String label = "• " + niceName;
                        int modColor = blocked ? 0xFF555555 : ReforgeUtil.getColorForModifier(id);
                        int maxTextWidth = 120;
                        String trimmed = textRenderer.trimToWidth(label, maxTextWidth);
                        int textX = listX + 5;
                        int textY = modY + 2;
                        context.drawText(textRenderer, Text.literal(trimmed), textX, textY, modColor, false);

                        if (id.equals(targetModifier) || targetGroupModifiers.contains(id)) {
                            Identifier lockIcon = Identifier.of("kevs", "textures/gui/lock.png");
                            int iconSize = 8;
                            int textWidth = textRenderer.getWidth(trimmed);
                            int iconX = textX + textWidth + 4;
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

                boolean blocked = producible != null && !producible.contains(id);
                String label = "• " + niceName;
                int modColor = blocked ? 0xFF555555 : ReforgeUtil.getColorForModifier(id);
                int maxTextWidth = 120;
                String trimmed = textRenderer.trimToWidth(label, maxTextWidth);
                int textX = listX + 5;
                int textY = modY + 2;
                context.drawText(textRenderer, Text.literal(trimmed), textX, textY, modColor, false);

                if (id.equals(targetModifier) || targetGroupModifiers.contains(id)) {
                    Identifier lockIcon = Identifier.of("kevs", "textures/gui/lock.png");
                    int iconSize = 8;
                    int textWidth = textRenderer.getWidth(trimmed);
                    int iconX = textX + textWidth + 4;
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

    private void renderPreviewPanel(DrawContext context) {
        int[] bounds = previewPanelBounds();
        if (bounds == null) return;
        int px = bounds[0], py = bounds[1], pw = bounds[2], ph = bounds[3];

        List<Text> lines = buildPreviewTooltip(buildPreviewStack());

        context.fill(px, py, px + pw, py + ph, 0xF0100010);
        context.drawBorder(px, py, pw, ph, (draggingPreview || resizingPreview) ? 0xFF8888FF : 0xFF4A2A6A);

        context.getMatrices().push();
        context.getMatrices().translate(px, py, 0);
        context.getMatrices().scale(previewScale, previewScale, 1.0f);

        int unscaledW = Math.round(pw / previewScale);
        int handleColor = (draggingPreview || resizingPreview) ? 0xFFAAAAFF : 0xFF6A4A8A;
        context.fill(1, 1, unscaledW - 1, PANEL_HANDLE_H, handleColor & 0x66FFFFFF | 0x40000000);
        context.drawText(this.textRenderer, Text.literal(previewHandleText()).styled(s -> s.withColor(Formatting.GRAY)),
                PANEL_PAD, 1, 0xFFAAAAAA, false);

        boolean shift = TieredKeybinds.descriptionsHeld();
        int ly = PANEL_HANDLE_H + PANEL_PAD - 2;
        int plateH = ImprintPlatesComponent.plateHeight();
        int platePad = ImprintPlatesComponent.padX();
        int rowX = PANEL_PAD;
        boolean inPlateRow = false;
        for (int li = 0; li < lines.size(); li++) {
            Text line = lines.get(li);
            boolean isMarker = ReforgeMaterialTooltip.isImprintMarker(line);

            if (!isMarker && inPlateRow) {
                ly += plateH + 2;
                rowX = PANEL_PAD;
                inPlateRow = false;
            }
            if (isMarker) {
                if (shift) {

                    if (inPlateRow) { ly += plateH + 2; rowX = PANEL_PAD; inPlateRow = false; }
                    Text desc = ReforgeMaterialTooltip.imprintDescriptionLine(line, null);
                    if (desc != null) {
                        for (var wrapped : this.textRenderer.wrapLines(desc, unscaledW - PANEL_PAD * 2)) {
                            context.drawText(this.textRenderer, wrapped, PANEL_PAD, ly, 0xFFFFFFFF, true);
                            ly += PANEL_LINE_H;
                        }
                    }
                } else {
                    var plate = ReforgeMaterialTooltip.markerToPlate(line);
                    if (plate != null) {
                        int pw2 = platePad + this.textRenderer.getWidth(plate.label()) + platePad;

                        if (inPlateRow && rowX + pw2 > unscaledW - PANEL_PAD) {
                            ly += plateH + 2;
                            rowX = PANEL_PAD;
                        }
                        ImprintPlatesComponent.drawStandalonePlate(
                                context, this.textRenderer, rowX, ly - 1, plate.label(), plate.fillColor());
                        rowX += pw2 + 3;
                        inPlateRow = true;
                    }
                }
                continue;
            }
            for (var wrapped : this.textRenderer.wrapLines(line, unscaledW - PANEL_PAD * 2)) {
                context.drawText(this.textRenderer, wrapped, PANEL_PAD, ly, 0xFFFFFFFF, true);
                ly += PANEL_LINE_H;
            }
        }

        if (inPlateRow) ly += plateH + 2;
        context.getMatrices().pop();

        int gx = px + pw - PREVIEW_RESIZE_GRIP;
        int gy = py + ph - PREVIEW_RESIZE_GRIP;
        int gripColor = resizingPreview ? 0xFFAAAAFF : 0xFF6A4A8A;
        for (int d = 0; d < PREVIEW_RESIZE_GRIP; d += 2) {
            context.fill(gx + d, py + ph - 1, gx + PREVIEW_RESIZE_GRIP, py + ph, gripColor);
            context.fill(px + pw - 1, gy + d, px + pw, gy + PREVIEW_RESIZE_GRIP, gripColor);
        }
    }

    private boolean isOverResizeGrip(double mouseX, double mouseY, int[] bounds) {
        int gx = bounds[0] + bounds[2] - PREVIEW_RESIZE_GRIP;
        int gy = bounds[1] + bounds[3] - PREVIEW_RESIZE_GRIP;
        return mouseX >= gx && mouseX <= bounds[0] + bounds[2] && mouseY >= gy && mouseY <= bounds[1] + bounds[3];
    }

    private String previewHandleText() {
        return "⋮⋮⋮ " + Text.translatable("screen.tiered.reforge.preview.drag").getString()
                + " • " + Text.translatable("screen.tiered.reforge.preview.resize").getString();
    }

    @Nullable
    private int[] previewPanelContentSize() {
        ItemStack preview = buildPreviewStack();
        if (preview == null) return null;
        List<Text> lines = buildPreviewTooltip(preview);
        boolean shift = TieredKeybinds.descriptionsHeld();
        int platePad = ImprintPlatesComponent.padX();
        int w = 0;
        int runWidth = 0;
        for (Text t : lines) {
            boolean isMarker = ReforgeMaterialTooltip.isImprintMarker(t);

            if (shift && isMarker) {
                Text desc = ReforgeMaterialTooltip.imprintDescriptionLine(t, null);
                if (desc != null) { w = Math.max(w, this.textRenderer.getWidth(desc)); continue; }
            }
            if (!shift && isMarker) {

                var plate = ReforgeMaterialTooltip.markerToPlate(t);
                if (plate != null) {
                    int pw2 = platePad + this.textRenderer.getWidth(plate.label()) + platePad;
                    runWidth += (runWidth == 0 ? 0 : 3) + pw2;
                    w = Math.max(w, runWidth);
                    continue;
                }
            }
            runWidth = 0;
            w = Math.max(w, this.textRenderer.getWidth(t));
        }

        w = Math.min(w, MAX_PREVIEW_PLATE_W);
        w = Math.max(w, this.textRenderer.getWidth(previewHandleText()));
        int pw = w + PANEL_PAD * 2;

        int wrapBound = pw - PANEL_PAD;
        int textWrapBound = pw - PANEL_PAD * 2;
        int plateH = ImprintPlatesComponent.plateHeight();
        int h = 0;
        int runRows = 0;
        int rowX = PANEL_PAD;
        for (Text t : lines) {
            boolean isMarker = ReforgeMaterialTooltip.isImprintMarker(t);
            if (!isMarker && runRows > 0) { h += runRows * (plateH + 2); runRows = 0; rowX = PANEL_PAD; }
            if (isMarker) {
                if (shift) {
                    Text desc = ReforgeMaterialTooltip.imprintDescriptionLine(t, null);
                    int rows = desc == null ? 0 : Math.max(1, this.textRenderer.wrapLines(desc, textWrapBound).size());
                    h += rows * PANEL_LINE_H;
                    continue;
                }
                var plate = ReforgeMaterialTooltip.markerToPlate(t);
                if (plate == null) continue;
                int pw2 = platePad + this.textRenderer.getWidth(plate.label()) + platePad;
                if (runRows == 0) { runRows = 1; rowX = PANEL_PAD + pw2 + 3; }
                else if (rowX + pw2 > wrapBound) { runRows++; rowX = PANEL_PAD + pw2 + 3; }
                else rowX += pw2 + 3;
            } else {
                h += Math.max(1, this.textRenderer.wrapLines(t, textWrapBound).size()) * PANEL_LINE_H;
            }
        }

        int trailing;
        if (runRows > 0) {
            trailing = 2;
            h += runRows * (plateH + 2);
        } else {
            trailing = Math.max(0, PANEL_LINE_H - this.textRenderer.fontHeight);
        }
        int ph = PANEL_HANDLE_H + PANEL_PAD - 2 + h - trailing + PANEL_PAD;
        return new int[]{pw, ph};
    }

    @Nullable
    private int[] previewPanelBounds() {
        int[] size = previewPanelContentSize();
        if (size == null) return null;

        if (previewPanelX == Integer.MIN_VALUE) {

            previewPanelX = this.x + this.backgroundWidth + 12;
            previewPanelY = this.y + 8;
        }

        int pw = Math.round(size[0] * previewScale);
        int ph = Math.round(size[1] * previewScale);
        return new int[]{previewPanelX, previewPanelY, pw, ph};
    }

    private boolean isOverPreviewPanel(double mouseX, double mouseY) {
        int[] b = previewPanelBounds();
        if (b == null) return false;
        return mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3];
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
        stopAutoReforge();
        handler.removeListener(this);
    }

    @Override public void onPropertyUpdate(ScreenHandler handler, int property, int value) {}
    @Override
    public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
        if (slotId == 1) {

            if (!ItemStack.areItemsEqual(stack, lastSeenStack)) {
                lastSeenStack = stack.copy();

                if (stack.isEmpty()) {
                    stopAutoReforge();
                }

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

                    int color = ReforgeUtil.getColorForModifier(newId);

                    if (isTargetAchieved(newId)) {
                        modifierAchieved = true;
                        stopAutoReforge();
                        Text achievedText;
                        if (!targetModifierGroups.isEmpty()) {
                            String achievedGroup = findAchievedGroup(newId);
                            if (achievedGroup != null) {
                                achievedText = Text.translatable("screen.tiered.reforge.achieved_groups", capitalize(achievedGroup));
                            } else {
                                achievedText = Text.translatable("screen.tiered.reforge.achieved_modifier");
                            }
                        } else {
                            achievedText = Text.translatable("screen.tiered.reforge.achieved_modifier");
                        }
                        floatingTexts.add(new FloatingText(achievedText, 0x55FF55, x, y));
                        triggerReforgeEffect(0x55FF55, true);
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
                    floatingTexts.add(new FloatingText(displayText, color, x, y));
                    triggerReforgeEffect(color, false);

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
