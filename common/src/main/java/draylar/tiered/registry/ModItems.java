package draylar.tiered.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import draylar.tiered.Tiered;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.TuningIngotConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create("tiered_more", Registries.ITEM);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create("tiered_more", Registries.CREATIVE_MODE_TAB);

    public static Item BLESSED_SCROLL;
    public static Item SPECIAL_TUNING_INGOT;

    public static final Map<String, Item> TUNING_INGOTS = new HashMap<>();
    public static final Map<String, Item> RUNES = new HashMap<>();
    public static CreativeModeTab TIERED_TAB;

    private static final String[] RUNE_NAMES = {
            "rune_verdant", "rune_obsidian", "rune_amber", "rune_steel", "rune_arcane",
            "rune_crimson", "rune_sandstone", "rune_ember", "rune_frost", "rune_shadow"
    };

    private static final List<String> EXTRA_RUNE_NAMES = new ArrayList<>();

    private static final Map<String, RegistrySupplier<Item>> TUNING_INGOT_SUPPLIERS = new HashMap<>();
    private static final Map<String, RegistrySupplier<Item>> RUNE_SUPPLIERS = new HashMap<>();
    private static RegistrySupplier<Item> blessedScrollSupplier;
    private static RegistrySupplier<Item> specialTuningIngotSupplier;
    private static RegistrySupplier<CreativeModeTab> tieredTabSupplier;
    private static boolean registered = false;

    public static void addRune(String runeName) {
        if (runeName != null && !runeName.isBlank()
                && !EXTRA_RUNE_NAMES.contains(runeName) && !RUNES.containsKey(runeName)) {
            EXTRA_RUNE_NAMES.add(runeName);
        }
    }

    // Queues item creation via Architectury's DeferredRegister so the actual Registry.register
    // call happens through RegisterEvent, not directly here. Must run once ConfigInit has loaded
    // (for CUSTOM_TUNING_INGOTS) but before the ITEM registry freezes - i.e. during mod
    // construction, same timing as ModDataComponents/CustomEntityAttributes.
    public static void register() {
        if (registered) return;
        registered = true;

        for (TuningIngotConfig entry : ConfigInit.CUSTOM_TUNING_INGOTS) {
            String group = entry.group;
            RegistrySupplier<Item> supplier = ITEMS.register(
                    Tiered.id("tuning_ingot_" + group.toLowerCase(Locale.ROOT)),
                    () -> new TuningIngotItem(
                            new Item.Properties().rarity(Rarity.EPIC),
                            group, entry.color
                    ) {
                        @Override
                        public String getDescriptionId() {
                            return "item.tiered.tuning_ingot";
                        }
                    }
            );
            TUNING_INGOT_SUPPLIERS.put(group, supplier);
        }

        blessedScrollSupplier = ITEMS.register(Tiered.id("blessed_scroll"),
                () -> new BlessedScrollItem(new Item.Properties().rarity(Rarity.EPIC)));

        specialTuningIngotSupplier = ITEMS.register(Tiered.id("special_tuning_ingot"),
                () -> new SpecialTuningIngotItem(new Item.Properties().rarity(Rarity.EPIC)) {
                    @Override
                    public String getDescriptionId() {
                        return "item.tiered.special_tuning_ingot";
                    }
                });

        List<String> allRunes = new ArrayList<>(Arrays.asList(RUNE_NAMES));
        allRunes.addAll(EXTRA_RUNE_NAMES);
        for (String runeName : allRunes) {
            if (RUNE_SUPPLIERS.containsKey(runeName)) continue;
            RegistrySupplier<Item> supplier = ITEMS.register(Tiered.id(runeName),
                    () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
            RUNE_SUPPLIERS.put(runeName, supplier);
        }

        ITEMS.register();

        // CreativeModeTab.Builder's icon/displayItems callbacks are lazy (evaluated when the tab
        // is actually opened), so this can be queued immediately, before any Item RegistrySupplier
        // above has resolved. Registered via DeferredRegister (not a direct Registry.register call)
        // since CREATIVE_MODE_TAB freezes on NeoForge even earlier than ITEM/ATTRIBUTE - it must go
        // through RegisterEvent like every other registry, not be inserted directly at any fixed point.
        tieredTabSupplier = TABS.register(Tiered.id("tab"), () -> CreativeTabRegistry.create(builder -> {
            builder.icon(() -> new ItemStack(TUNING_INGOTS.values().iterator().next()));
            builder.title(Component.translatable("itemGroup.tiered.tab"));
            builder.displayItems((context, entries) -> {
                TUNING_INGOTS.values().forEach(entries::accept);
                entries.accept(BLESSED_SCROLL);
                if (SPECIAL_TUNING_INGOT != null) entries.accept(SPECIAL_TUNING_INGOT);
                RUNES.values().forEach(entries::accept);
            });
        }));
        TABS.register();
    }

    // Resolves the RegistrySupplier values into plain Item fields (populating TUNING_INGOTS,
    // BLESSED_SCROLL, etc. that the tab's lazy callbacks above read) and appends the tuning
    // ingots to vanilla's INGREDIENTS tab. Must run after RegisterEvent has actually populated
    // the ITEM registry (register() above only queues); safe to call from LifecycleEvent.SETUP
    // like the rest of Tiered.init().
    public static void init() {
        TUNING_INGOT_SUPPLIERS.forEach((group, supplier) -> TUNING_INGOTS.put(group, supplier.get()));
        BLESSED_SCROLL = blessedScrollSupplier.get();
        SPECIAL_TUNING_INGOT = specialTuningIngotSupplier.get();
        RUNE_SUPPLIERS.forEach((name, supplier) -> RUNES.put(name, supplier.get()));
        TIERED_TAB = tieredTabSupplier.get();

        List<Item> ingredientsAdditions = new ArrayList<>(TUNING_INGOTS.values());
        if (SPECIAL_TUNING_INGOT != null) ingredientsAdditions.add(SPECIAL_TUNING_INGOT);
        CreativeTabRegistry.append(CreativeModeTabs.INGREDIENTS, ingredientsAdditions.toArray(new Item[0]));
    }
}
