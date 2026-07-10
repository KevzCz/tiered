package draylar.tiered.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import draylar.tiered.Tiered;
import org.lwjgl.glfw.GLFW;

public final class TooltipKeysConfig {

    private static final File FILE = new File("config/tiered_more/tooltip-keys.json5");

    public static int detailsKey = GLFW.GLFW_KEY_LEFT_ALT;
    public static int descriptionsKey = GLFW.GLFW_KEY_LEFT_CONTROL;
    public static int worksWithKey = GLFW.GLFW_KEY_LEFT_SHIFT;

    private TooltipKeysConfig() {
    }

    public static void load() {
        if (!FILE.exists()) {
            writeDefault();
            return;
        }
        try {
            String raw = Files.readString(FILE.toPath(), StandardCharsets.UTF_8);
            Map<String, String> kv = parse(raw);
            String details = kv.getOrDefault("details_key", "LEFT_ALT");
            String descriptions = kv.getOrDefault("descriptions_key", "LEFT_CONTROL");
            String worksWith = kv.getOrDefault("works_with_key", "LEFT_SHIFT");
            detailsKey = keyCode(details);
            descriptionsKey = keyCode(descriptions);
            worksWithKey = keyCode(worksWith);
        } catch (IOException e) {
            Tiered.LOGGER.error("Failed to read tooltip-keys.json5, using defaults", e);
        }
    }

    private static Map<String, String> parse(String raw) {
        Map<String, String> out = new HashMap<>();
        for (String line : raw.split("\n")) {
            String s = line.trim();
            int comment = s.indexOf("//");
            if (comment >= 0) s = s.substring(0, comment).trim();
            if (s.isEmpty() || s.equals("{") || s.equals("}")) continue;
            s = s.replace(",", "").trim();
            String[] parts = s.split(":", 2);
            if (parts.length != 2) continue;
            String k = parts[0].trim().replace("\"", "");
            String v = parts[1].trim().replace("\"", "");
            if (!k.isEmpty() && !v.isEmpty()) out.put(k, v);
        }
        return out;
    }

    private static int keyCode(String name) {
        String n = name.trim().toUpperCase();
        if (n.equals("NONE")) return -1;
        try {
            Field f = GLFW.class.getField("GLFW_KEY_" + n);
            return f.getInt(null);
        } catch (ReflectiveOperationException e) {
            Tiered.LOGGER.warn("Unknown tooltip key '{}', falling back to LEFT_ALT", name);
            return GLFW.GLFW_KEY_LEFT_ALT;
        }
    }

    private static void writeDefault() {
        FILE.getParentFile().mkdirs();
        String content = """
                {
                  // Tooltip modifier keys for the imprint/material tooltip. These are NOT vanilla keybinds —
                  // editing them here will not touch your Options > Controls bindings.
                  //
                  // Accepted values are GLFW key names WITHOUT the GLFW_KEY_ prefix. Examples:
                  //   LEFT_ALT, RIGHT_ALT, LEFT_CONTROL, RIGHT_CONTROL, LEFT_SHIFT, RIGHT_SHIFT,
                  //   TAB, SPACE, CAPS_LOCK, ENTER, A, B, C ... Z, 0 ... 9, F1 ... F12
                  // Use "NONE" to disable a modifier entirely.
                  //
                  // Full key list: https://www.glfw.org/docs/latest/group__keys.html (drop the GLFW_KEY_ prefix)

                  // Hold to show the detailed imprint view:
                  "details_key": "LEFT_ALT",

                  // Hold to show imprint descriptions:
                  "descriptions_key": "LEFT_CONTROL",

                  // Hold to show what an imprint works with (item tags):
                  "works_with_key": "LEFT_SHIFT"
                }
                """;
        try {
            Files.writeString(FILE.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Tiered.LOGGER.error("Failed to write default tooltip-keys.json5", e);
        }
    }
}
