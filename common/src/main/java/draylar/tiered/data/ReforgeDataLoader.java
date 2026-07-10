package draylar.tiered.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.platform.Platform;
import draylar.tiered.TieredClient;
import draylar.tiered.TieredServer;
import draylar.tiered.compat.ATCCompat;
import net.fabricmc.api.EnvType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Consumer;

public class ReforgeDataLoader implements ResourceManagerReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TieredZ");

    private final List<ResourceLocation> reforgeIdentifiers = new ArrayList<>();
    private final Map<ResourceLocation, List<Item>> reforgeBaseMap = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        reforgeIdentifiers.clear();
        reforgeBaseMap.clear();

        resourceManager.listResources("reforge_items", id -> id.getPath().endsWith(".json")).forEach((id, resourceRef) -> {
            try (InputStream stream = resourceRef.open()) {
                JsonObject data = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

                List<String> baseRaw = new ArrayList<>();
                List<String> itemRaw = new ArrayList<>();

                data.getAsJsonArray("base").forEach(el -> baseRaw.add(el.getAsString()));
                data.getAsJsonArray("items").forEach(el -> itemRaw.add(el.getAsString()));

                Runnable task = () -> {
                    List<Item> baseItems = new ArrayList<>();

                    for (String entry : baseRaw) {
                        if (isCustomAccessoryTag(entry)) {
                            forEachMatchingAccessoryItem(entry, baseItems::add);
                        } else if (entry.startsWith("#")) {
                            ResourceLocation tagId = ResourceLocation.parse(entry.substring(1));
                            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
                            BuiltInRegistries.ITEM.getTag(tagKey).ifPresentOrElse(
                                    list -> list.forEach(e -> baseItems.add(e.value())),
                                    () -> LOGGER.warn("Base tag '{}' not found in file '{}'", tagId, id)
                            );
                        } else {
                            ResourceLocation itemId = ResourceLocation.parse(entry);
                            Item item = BuiltInRegistries.ITEM.get(itemId);
                            if (!item.toString().equals("air")) {
                                baseItems.add(item);
                            } else {
                                LOGGER.warn("Invalid base item '{}' in {}", entry, id);
                            }
                        }
                    }

                    for (String itemEntry : itemRaw) {
                        if (isCustomAccessoryTag(itemEntry)) {
                            forEachMatchingAccessoryItem(itemEntry, item -> {
                                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                                reforgeIdentifiers.add(itemId);
                                reforgeBaseMap.put(itemId, new ArrayList<>(baseItems));
                                LOGGER.debug("[Tiered] Added reforge custom item: {} -> {}", itemEntry, itemId);
                            });
                        } else if (itemEntry.startsWith("#")) {
                            ResourceLocation tagId = ResourceLocation.parse(itemEntry.substring(1));
                            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
                            BuiltInRegistries.ITEM.getTag(tagKey).ifPresentOrElse(
                                    list -> {
                                        for (Holder<Item> entry : list) {
                                            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.value());
                                            reforgeIdentifiers.add(itemId);
                                            reforgeBaseMap.put(itemId, new ArrayList<>(baseItems));
                                            LOGGER.debug("[Tiered] Added reforge tag item: {} -> {}", tagId, itemId);
                                        }
                                    },
                                    () -> LOGGER.warn("Target tag '{}' not found in file '{}'", tagId, id)
                            );
                        } else {
                            ResourceLocation itemId = ResourceLocation.parse(itemEntry);
                            Item item = BuiltInRegistries.ITEM.get(itemId);
                            if (!item.toString().equals("air")) {
                                reforgeIdentifiers.add(itemId);
                                reforgeBaseMap.put(itemId, new ArrayList<>(baseItems));
                                LOGGER.debug("[Tiered] Added reforge item: {}", itemId);
                            } else {
                                LOGGER.warn("Invalid target item '{}' in {}", itemEntry, id);
                            }
                        }
                    }
                };

                if (Platform.getEnv() == EnvType.CLIENT) {
                    TieredClient.TASK_QUEUE.add(task);
                } else {
                    TieredServer.TASK_QUEUE.add(task);
                }

            } catch (Exception e) {
                LOGGER.error("Failed to load reforge config '{}': {}", id, e.toString());
                e.printStackTrace();
            }
        });
    }

    // #tiered:all_trinkets_items / #tiered:all_curios_items are virtual tags with no real
    // registered HolderSet (Trinkets/Curios have no umbrella "all items" tag of their own) -
    // recognized here the same way ItemVerifier/ImprintSlotLoader do, but since there's no tag to
    // pull a HolderSet from, matching items are found by scanning the full item registry instead.
    private static boolean isCustomAccessoryTag(String entry) {
        if (!entry.startsWith("#")) return false;
        String withoutHash = entry.substring(1);
        return withoutHash.equals("tiered:all_trinkets_items") || withoutHash.equals("tiered:all_curios_items");
    }

    private static void forEachMatchingAccessoryItem(String entry, Consumer<Item> consumer) {
        boolean trinkets = entry.substring(1).equals("tiered:all_trinkets_items");
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            boolean matches = trinkets ? ATCCompat.isAnyTrinketItem(stack) : ATCCompat.isAnyCurioItem(stack);
            if (matches) consumer.accept(item);
        }
    }

    public List<Item> getReforgeBaseItems(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return reforgeBaseMap.getOrDefault(id, new ArrayList<>());
    }

    public void putReforgeBaseItems(ResourceLocation id, List<Item> items) {
        reforgeBaseMap.put(id, items);
    }

    public void clearReforgeBaseItems() {
        reforgeBaseMap.clear();
    }

    public List<ResourceLocation> getReforgeIdentifiers() {
        return reforgeIdentifiers;
    }
}
