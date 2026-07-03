package draylar.tiered.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import draylar.tiered.api.ReforgeMaterial;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ReforgeMaterialLoader extends JsonDataLoader implements SimpleSynchronousResourceReloadListener {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Logger LOGGER = LogManager.getLogger();

    private Map<Identifier, ReforgeMaterial> materials = new HashMap<>();

    private Map<String, ReforgeMaterial> tagMaterials = new HashMap<>();
    private boolean tagsExpanded = false;

    public ReforgeMaterialLoader() {
        super(GSON, "reforge_material");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> loader, ResourceManager manager, Profiler profiler) {
        Map<Identifier, ReforgeMaterial> read = Maps.newHashMap();
        Map<String, ReforgeMaterial> readTags = Maps.newHashMap();

        for (Map.Entry<Identifier, JsonElement> entry : loader.entrySet()) {
            Identifier fileId = entry.getKey();
            try {
                ReforgeMaterial material = GSON.fromJson(entry.getValue(), ReforgeMaterial.class);
                if (material == null || material.getItem() == null) {
                    LOGGER.error("Reforge material {} is missing an 'item' field", fileId);
                    continue;
                }
                String target = material.getItem();
                if (target.startsWith("#")) {
                    readTags.put(target.substring(1), material);
                } else {
                    read.put(Identifier.of(target), material);
                }
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error("Parsing error loading reforge material {}", fileId, exception);
            }
        }

        materials = read;
        tagMaterials = readTags;
        tagsExpanded = false;
        LOGGER.info("Loaded {} reforge materials ({} tag-based)", read.size(), readTags.size());
    }

    private void expandTagsIfNeeded() {
        if (tagsExpanded || tagMaterials.isEmpty()) {
            tagsExpanded = true;
            return;
        }
        for (Map.Entry<String, ReforgeMaterial> entry : tagMaterials.entrySet()) {
            TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, Identifier.of(entry.getKey()));
            Registries.ITEM.getEntryList(tagKey).ifPresent(list -> {
                for (RegistryEntry<Item> itemEntry : list) {
                    materials.putIfAbsent(Registries.ITEM.getId(itemEntry.value()), entry.getValue());
                }
            });
        }
        tagsExpanded = true;
    }

    public ReforgeMaterial getMaterial(Item item) {
        expandTagsIfNeeded();
        return materials.get(Registries.ITEM.getId(item));
    }

    public Map<Identifier, ReforgeMaterial> getMaterials() {
        expandTagsIfNeeded();
        return materials;
    }

    public void setMaterials(Map<Identifier, ReforgeMaterial> incoming) {
        materials = new HashMap<>(incoming);
        tagMaterials = new HashMap<>();
        tagsExpanded = true;
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("tiered", "reforge_material");
    }

    @Override
    public void reload(ResourceManager resourceManager) {
    }
}
