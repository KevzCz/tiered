package draylar.tiered.api.imprint.ability;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public class AbilityBinding {

    @Nullable @SerializedName("mastered_when") private String masteredWhen;
    @Nullable private String ability;
    @Nullable private Map<String, Float> params;
    @Nullable @SerializedName("tier_params") @JsonAdapter(TierParamsDeserializer.class)
    private List<Map<String, Float>> tierParams;
    @Nullable private Plate plate;

    public AbilityBinding() {
    }

    @Nullable public String getMasteredWhen() { return masteredWhen; }
    @Nullable public String getAbility() { return ability; }

    public Map<String, Float> getParams() { return params == null ? Map.of() : params; }

    public Map<String, Float> getParams(int tier) {
        if (tierParams != null && !tierParams.isEmpty()) {
            int idx = Math.max(0, Math.min(tier - 1, tierParams.size() - 1));
            Map<String, Float> m = tierParams.get(idx);
            return m == null ? Map.of() : m;
        }
        return getParams();
    }

    @Nullable public Plate getPlate() { return plate; }

    public AbilityBinding masteredWhen(String v) { this.masteredWhen = v; return this; }
    public AbilityBinding ability(String v) { this.ability = v; return this; }
    public AbilityBinding param(String key, float value) {
        if (params == null) params = new HashMap<>();
        params.put(key, value);
        return this;
    }
    public AbilityBinding tierParam(int tier, String key, float value) {
        if (tierParams == null) tierParams = new ArrayList<>();
        while (tierParams.size() < tier) tierParams.add(new HashMap<>());
        tierParams.get(tier - 1).put(key, value);
        return this;
    }
    public AbilityBinding plate(Plate v) { this.plate = v; return this; }

    static final class TierParamsDeserializer
            implements JsonDeserializer<List<Map<String, Float>>>, JsonSerializer<List<Map<String, Float>>> {

        @Override
        public JsonElement serialize(List<Map<String, Float>> src, Type type, JsonSerializationContext ctx) {
            JsonObject obj = new JsonObject();
            for (int i = 0; i < src.size(); i++) {
                Map<String, Float> map = src.get(i);
                if (map == null || map.isEmpty()) continue;
                JsonObject tierObj = new JsonObject();
                for (Map.Entry<String, Float> e : map.entrySet()) tierObj.addProperty(e.getKey(), e.getValue());
                obj.add("tier_" + (i + 1), tierObj);
            }
            return obj;
        }

        @Override
        public List<Map<String, Float>> deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            if (json.isJsonArray()) {

                JsonArray arr = json.getAsJsonArray();
                List<Map<String, Float>> result = new ArrayList<>(arr.size());
                for (JsonElement el : arr) result.add(parseFloatMap(el.getAsJsonObject()));
                return result;
            } else if (json.isJsonObject()) {

                JsonObject obj = json.getAsJsonObject();
                int maxTier = 0;
                for (String key : obj.keySet()) {
                    if (key.startsWith("tier_")) {
                        try { maxTier = Math.max(maxTier, Integer.parseInt(key.substring(5))); }
                        catch (NumberFormatException ignored) {}
                    }
                }
                List<Map<String, Float>> result = new ArrayList<>(maxTier);
                for (int i = 0; i < maxTier; i++) result.add(null);
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    if (entry.getKey().startsWith("tier_")) {
                        try {
                            int t = Integer.parseInt(entry.getKey().substring(5));
                            result.set(t - 1, parseFloatMap(entry.getValue().getAsJsonObject()));
                        } catch (NumberFormatException ignored) {}
                    }
                }

                result.replaceAll(m -> m == null ? Map.of() : m);
                return result;
            }
            throw new JsonParseException("tier_params must be a JSON array or object, got: " + json);
        }

        private static Map<String, Float> parseFloatMap(JsonObject obj) {
            Map<String, Float> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                map.put(e.getKey(), e.getValue().getAsFloat());
            }
            return map;
        }
    }

    public static class Plate {

        @Nullable @SerializedName("name_key") private String nameKey;
        @Nullable @SerializedName("line_key") private String lineKey;
        @Nullable private String color;

        public Plate() {
        }

        @Nullable public String getNameKey() { return nameKey; }
        @Nullable public String getLineKey() { return lineKey; }
        @Nullable public String getColor() { return color; }

        public Plate nameKey(String v) { this.nameKey = v; return this; }
        public Plate lineKey(String v) { this.lineKey = v; return this; }
        public Plate color(String v) { this.color = v; return this; }
    }
}
