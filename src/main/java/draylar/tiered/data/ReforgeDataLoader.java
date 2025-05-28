package draylar.tiered.data;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import draylar.tiered.TieredClient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class ReforgeDataLoader implements SimpleSynchronousResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TieredZ");

    private List<Identifier> reforgeIdentifiers = new ArrayList<>();
    private Map<Identifier, List<Item>> reforgeBaseMap = new HashMap<>();

    @Override
    public Identifier getFabricId() {
        return Identifier.of("tiered", "reforge_loader");
    }

    @Override
    public void reload(ResourceManager resourceManager) {
        reforgeIdentifiers.clear();
        reforgeBaseMap.clear();

        resourceManager.findResources("reforge_items", id -> id.getPath().endsWith(".json")).forEach((id, resourceRef) -> {
            try {
                InputStream stream = resourceRef.getInputStream();
                JsonObject data = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

                // Store string identifiers temporarily to resolve later
                List<String> baseRaw = new ArrayList<>();
                List<String> itemRaw = new ArrayList<>();

                data.getAsJsonArray("base").forEach(el -> baseRaw.add(el.getAsString()));
                data.getAsJsonArray("items").forEach(el -> itemRaw.add(el.getAsString()));

                TieredClient.TASK_QUEUE.add(() -> {
                    List<Item> baseItems = new ArrayList<>();

                    for (String entry : baseRaw) {
                        if (entry.startsWith("#")) {
                            Identifier tagId = Identifier.of(entry.substring(1));
                            Registries.ITEM.getEntryList(TagKey.of(RegistryKeys.ITEM, tagId)).ifPresentOrElse(
                                    list -> list.forEach(e -> baseItems.add(e.value())),
                                    () -> LOGGER.warn("Base item tag {} not found in {}", tagId, id)
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
                            Registries.ITEM.getEntryList(TagKey.of(RegistryKeys.ITEM, tagId)).ifPresentOrElse(
                                    list -> {
                                        for (RegistryEntry<Item> entry : list) {
                                            Identifier itemId = Registries.ITEM.getId(entry.value());
                                            reforgeIdentifiers.add(itemId);
                                            reforgeBaseMap.put(itemId, new ArrayList<>(baseItems));
                                        }
                                    },
                                    () -> LOGGER.warn("Target item tag {} not found in {}", tagId, id)
                            );
                        } else {
                            Identifier itemId = Identifier.of(itemEntry);
                            Item item = Registries.ITEM.get(itemId);
                            if (!item.toString().equals("air")) {
                                reforgeIdentifiers.add(itemId);
                                reforgeBaseMap.put(itemId, new ArrayList<>(baseItems));
                            } else {
                                LOGGER.warn("Invalid target item '{}' in {}", itemEntry, id);
                            }
                        }
                    }
                });

            } catch (Exception e) {
                LOGGER.error("Error occurred while loading resource {}. {}", id.toString(), e.toString());
            }
        });
    }


    public List<Item> getReforgeBaseItems(Item item) {
        ArrayList<Item> list = new ArrayList<Item>();
        if (reforgeBaseMap.containsKey(Registries.ITEM.getId(item))) {
            return reforgeBaseMap.get(Registries.ITEM.getId(item));
        }
        return list;
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
