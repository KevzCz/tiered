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
import draylar.tiered.config.ConfigInit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class ImprintDefinitionLoader extends SimpleJsonResourceReloadListener {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Logger LOGGER = LogManager.getLogger();

    private Map<String, DataImprint> imprints = new HashMap<>();
    private Map<String, ImprintDefinition> definitions = new HashMap<>();

    public ImprintDefinitionLoader() {
        super(GSON, "imprint");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loader, ResourceManager manager, ProfilerFiller profiler) {
        if (!ConfigInit.imprintsEffectsAndBehaviorsEnabled()) {
            imprints = new HashMap<>();
            definitions = new HashMap<>();
            return;
        }
        Map<String, DataImprint> readImprints = Maps.newHashMap();
        Map<String, ImprintDefinition> readDefs = Maps.newHashMap();

        for (Map.Entry<ResourceLocation, JsonElement> entry : loader.entrySet()) {
            ResourceLocation fileId = entry.getKey();
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

}
