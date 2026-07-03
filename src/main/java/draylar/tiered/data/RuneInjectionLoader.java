package draylar.tiered.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RuneInjectionLoader extends JsonDataLoader implements SimpleSynchronousResourceReloadListener {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogManager.getLogger();

    private List<RuneInjection> injections = new ArrayList<>();
    private ResourceManager lastManager = null;
    private final ResourceManager pendingManager = null;

    public RuneInjectionLoader() {
        super(GSON, "rune_injection");
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("tiered", "rune_injection");
    }

    @Override
    public void reload(ResourceManager manager) {
        loadFrom(manager);
    }

    public void loadFrom(ResourceManager manager) {
        if (manager == lastManager) return;
        lastManager = manager;
        Map<Identifier, JsonElement> raw = new HashMap<>();
        JsonDataLoader.load(manager, "rune_injection", GSON, raw);
        apply(raw, manager, null);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> loader, ResourceManager manager, Profiler profiler) {
        Map<Identifier, RuneInjection> read = Maps.newHashMap();
        for (Map.Entry<Identifier, JsonElement> entry : loader.entrySet()) {
            try {
                RuneInjection injection = GSON.fromJson(entry.getValue(), RuneInjection.class);
                if (injection != null && !injection.getPools().isEmpty()) {
                    read.put(entry.getKey(), injection);
                }
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error("Parsing error loading rune injection {}", entry.getKey(), exception);
            }
        }
        injections = new ArrayList<>(read.values());
        LOGGER.info("Loaded {} rune loot injections", injections.size());
    }

    public List<RuneInjection> getMatching(String tableId) {
        List<RuneInjection> out = new ArrayList<>();
        for (RuneInjection injection : injections) {
            if (injection.matchesTable(tableId)) out.add(injection);
        }
        return out;
    }
}
