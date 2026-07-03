package draylar.tiered.api.imprint;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImprintParamDisplay {

    private static final Map<String, String> DISPLAY = new HashMap<>();

    static {
        register("heal_fraction", "percent");
        register("health_threshold", "percent");
        register("threshold_multiplier", "raw");
        register("chance", "percent");
        register("min_damage", "percent");
        register("threshold_count", "raw");
        register("saturation_max_multiplier", "raw");
        register("amplifier", "amp_level");
        register("duration_ticks", "seconds");
        register("cooldown_ticks", "seconds");
        register("max_duration_ticks", "seconds");
        register("heal_ticks", "seconds");
        register("reset_ticks", "seconds");
    }

    private ImprintParamDisplay() {
    }

    public static void register(String paramKey, String display) {
        if (paramKey != null && display != null) DISPLAY.put(paramKey, display);
    }

    public static String displayOf(String paramKey) {
        return DISPLAY.getOrDefault(paramKey, "raw");
    }

    // {key}, {key%}, {key:sec|raw|amp|percent} tokens, order-independent alternative to positional %s.
    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z0-9_]+)(%|:[a-zA-Z]+)?}");

    public static String resolveNamedTokens(String raw, Map<String, Float> params) {
        if (raw == null || raw.indexOf('{') < 0) return null;
        Matcher m = TOKEN.matcher(raw);
        if (!m.find()) return null;
        StringBuilder sb = new StringBuilder();
        m.reset();
        while (m.find()) {
            String key = m.group(1);
            String hint = m.group(2); // "%" or ":<display>" or null
            String display = hintToDisplay(hint, key);
            Float value = params == null ? null : params.get(key);
            String replacement = value == null
                    ? "{" + key + "}" // leave unknown tokens visible for debugging
                    : String.valueOf(DataImprint.formatParamPublic(value, display));
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String resolveNamedTokensString(String raw, Map<String, String> tokens) {
        if (raw == null || raw.indexOf('{') < 0) return null;
        Matcher m = TOKEN.matcher(raw);
        if (!m.find()) return null;
        StringBuilder sb = new StringBuilder();
        m.reset();
        while (m.find()) {
            String key = m.group(1);
            String value = tokens == null ? null : tokens.get(key);
            String replacement = value == null ? "{" + key + "}" : value;
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String hintToDisplay(String hint, String key) {
        if (hint == null) return displayOf(key);
        if (hint.equals("%")) return "percent";
        String d = hint.substring(1); // drop leading ':'
        return switch (d) {
            case "sec", "secs", "seconds" -> "seconds";
            case "pct", "percent" -> "percent";
            case "amp" -> "amp_level";
            case "int", "raw" -> "raw";
            default -> displayOf(key);
        };
    }
}
