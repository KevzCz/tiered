package draylar.tiered.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import draylar.tiered.api.imprint.DataImprint;
import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintDefinition;
import draylar.tiered.api.imprint.ImprintRegistry;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ImprintDefinitionLoader extends JsonDataLoader implements SimpleSynchronousResourceReloadListener {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Logger LOGGER = LogManager.getLogger();

    private Map<String, DataImprint> imprints = new HashMap<>();
    private Map<String, ImprintDefinition> definitions = new HashMap<>();

    public ImprintDefinitionLoader() {
        super(GSON, "imprint");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> loader, ResourceManager manager, Profiler profiler) {
        Map<String, DataImprint> readImprints = Maps.newHashMap();
        Map<String, ImprintDefinition> readDefs = Maps.newHashMap();

        for (Map.Entry<Identifier, JsonElement> entry : loader.entrySet()) {
            Identifier fileId = entry.getKey();
            try {
                ImprintDefinition def = GSON.fromJson(entry.getValue(), ImprintDefinition.class);
                if (def == null) continue;
                String id = def.getId() != null ? def.getId() : fileId.toString();
                readImprints.put(id, new DataImprint(id, def));
                readDefs.put(id, def);
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error("Parsing error loading imprint definition {}", fileId, exception);
            }
        }

        imprints = readImprints;
        definitions = readDefs;
        LOGGER.info("Loaded {} data-defined imprints", imprints.size());
    }

    @Nullable
    public Imprint get(String id) {
        return imprints.get(id);
    }

    public Map<String, DataImprint> getImprints() {
        return imprints;
    }

    public Map<String, ImprintDefinition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String, ImprintDefinition> incoming) {
        Map<String, DataImprint> rebuilt = new HashMap<>();
        for (Map.Entry<String, ImprintDefinition> e : incoming.entrySet()) {
            rebuilt.put(e.getKey(), new DataImprint(e.getKey(), e.getValue()));
        }
        imprints = rebuilt;
        definitions = new HashMap<>(incoming);
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("tiered", "imprint");
    }

    @Override
    public void reload(ResourceManager resourceManager) {
    }
}
