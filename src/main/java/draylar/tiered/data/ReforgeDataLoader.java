package draylar.tiered.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import draylar.tiered.TieredClient;
import draylar.tiered.TieredServer;
import draylar.tiered.compat.AccessoryTagHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class ReforgeDataLoader implements SimpleSynchronousResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TieredZ");

    private final List<Identifier> reforgeIdentifiers = new ArrayList<>();
    private final Map<Identifier, List<Item>> reforgeBaseMap = new HashMap<>();

    @Override
    public Identifier getFabricId() {
        return Identifier.of("tiered", "reforge_loader");
    }

    @Override
    public void reload(ResourceManager resourceManager) {
        reforgeIdentifiers.clear();
        reforgeBaseMap.clear();

        resourceManager.findResources("reforge_items", id -> id.getPath().endsWith(".json")).forEach((id, resourceRef) -> {
            try (InputStream stream = resourceRef.getInputStream()) {
                JsonObject data = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

                List<String> baseRaw = new ArrayList<>();
                List<String> itemRaw = new ArrayList<>();

                data.getAsJsonArray("base").forEach(el -> baseRaw.add(el.getAsString()));
                data.getAsJsonArray("items").forEach(el -> itemRaw.add(el.getAsString()));

                Runnable task = () -> {
                    List<Item> baseItems = new ArrayList<>();

                    for (String entry : baseRaw) {
                        if (entry.startsWith("#")) {
                            Identifier tagId = Identifier.of(entry.substring(1));

                            if (tagId.toString().equals("tclayer:all_trinket_items")) {
                                AccessoryTagHandler.getAllTrinketItems().forEach(baseItems::add);
                                LOGGER.debug("[Tiered] Added {} accessory items as base items from virtual tag", AccessoryTagHandler.getAllTrinketItems().size());
                                continue;
                            }

                            TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, tagId);
                            Registries.ITEM.getEntryList(tagKey).ifPresentOrElse(
                                    list -> list.forEach(e -> baseItems.add(e.value())),
                                    () -> LOGGER.warn("Base tag '{}' not found in file '{}'", tagId, id)
                            );
                        } else {
                            Identifier itemId = Identifier.of(entry);
                            Item item = Registries.ITEM.get(itemId);
                            if (!item.toString().equals("air")) {
                                baseItems.add(item);
                            } else {
                                LOGGER.warn("Invalid base item '{}' in {}", entry, id);
                            }
                        }
                    }

                    for (String itemEntry : itemRaw) {
                        if (itemEntry.startsWith("#")) {
                            Identifier tagId = Identifier.of(itemEntry.substring(1));

                            if (tagId.toString().equals("tclayer:all_trinket_items")) {
                                AccessoryTagHandler.getAllTrinketItems().forEach(item -> {
                                    Identifier itemId = Registries.ITEM.getId(item);
                                    reforgeIdentifiers.add(itemId);
                                    reforgeBaseMap.put(itemId, new ArrayList<>(baseItems));
                                });
                                LOGGER.info("[Tiered] Added {} accessory items from virtual tag tclayer:all_trinket_items", AccessoryTagHandler.getAllTrinketItems().size());
                                continue;
                            }

                            TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, tagId);
                            Registries.ITEM.getEntryList(tagKey).ifPresentOrElse(
                                    list -> {
                                        for (RegistryEntry<Item> entry : list) {
                                            Identifier itemId = Registries.ITEM.getId(entry.value());
                                            reforgeIdentifiers.add(itemId);
                                            reforgeBaseMap.put(itemId, new ArrayList<>(baseItems));
                                            LOGGER.debug("[Tiered] Added reforge tag item: {} -> {}", tagId, itemId);
                                        }
                                    },
                                    () -> LOGGER.warn("Target tag '{}' not found in file '{}'", tagId, id)
                            );
                        } else {
                            Identifier itemId = Identifier.of(itemEntry);
                            Item item = Registries.ITEM.get(itemId);
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

                if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
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

    public List<Item> getReforgeBaseItems(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return reforgeBaseMap.getOrDefault(id, new ArrayList<>());
    }

    public void putReforgeBaseItems(Identifier id, List<Item> items) {
        reforgeBaseMap.put(id, items);
    }

    public void clearReforgeBaseItems() {
        reforgeBaseMap.clear();
    }

    public List<Identifier> getReforgeIdentifiers() {
        return reforgeIdentifiers;
    }
}