package draylar.tiered.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import draylar.tiered.api.ImprintSlotRule;
import draylar.tiered.util.ImprintSlots;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImprintSlotLoader extends JsonDataLoader implements SimpleSynchronousResourceReloadListener {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Logger LOGGER = LogManager.getLogger();

    private List<ImprintSlotRule> rules = new ArrayList<>();

    public ImprintSlotLoader() {
        super(GSON, "imprint_slots");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> loader, ResourceManager manager, Profiler profiler) {
        Map<Identifier, ImprintSlotRule> read = Maps.newHashMap();

        for (Map.Entry<Identifier, JsonElement> entry : loader.entrySet()) {
            Identifier fileId = entry.getKey();
            try {
                ImprintSlotRule rule = GSON.fromJson(entry.getValue(), ImprintSlotRule.class);
                if (rule == null || rule.getTarget() == null) {
                    LOGGER.error("Imprint slot rule {} is missing a 'target' field", fileId);
                    continue;
                }
                read.put(fileId, rule);
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error("Parsing error loading imprint slot rule {}", fileId, exception);
            }
        }

        rules = new ArrayList<>(read.values());
        LOGGER.info("Loaded {} imprint slot rules", rules.size());
    }

    @Nullable
    public ImprintSlotRule bestRule(Item item) {
        Identifier itemId = Registries.ITEM.getId(item);
        int bestSpecificity = -1;
        ImprintSlotRule best = null;
        for (ImprintSlotRule rule : rules) {
            if (!matches(rule, item, itemId)) continue;
            int spec = rule.specificity();
            if (spec > bestSpecificity) {
                bestSpecificity = spec;
                best = rule;
            }
        }
        return best;
    }

    public int datapackSlots(Item item) {
        ImprintSlotRule best = bestRule(item);
        return best == null ? 0 : best.getSlots();
    }

    public int datapackMaxSlots(Item item) {
        ImprintSlotRule best = bestRule(item);
        return best == null ? -1 : best.getMax();
    }

    private boolean matches(ImprintSlotRule rule, Item item, Identifier itemId) {
        String target = rule.getTarget();
        if (target == null) return false;
        if (target.equals("*")) return true;
        if (target.endsWith(":*")) {
            String namespace = target.substring(0, target.length() - 2);
            return itemId.getNamespace().equals(namespace);
        }
        if (target.startsWith("#")) {
            TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, Identifier.of(target.substring(1)));
            return item.getRegistryEntry().isIn(tagKey);
        }
        return itemId.equals(Identifier.tryParse(target));
    }

    public List<ImprintSlotRule> getRules() {
        return rules;
    }

    public void setRules(List<ImprintSlotRule> incoming) {
        rules = new ArrayList<>(incoming);
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("tiered", "imprint_slots");
    }

    @Override
    public void reload(ResourceManager resourceManager) {
    }
}
