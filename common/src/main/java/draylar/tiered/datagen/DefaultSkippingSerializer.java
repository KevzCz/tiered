package draylar.tiered.datagen;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

public final class DefaultSkippingSerializer<T> implements JsonSerializer<T> {

    private static final Gson CHECKER = new GsonBuilder().create();
    private final T defaultInstance;

    public DefaultSkippingSerializer(Class<T> clazz) {
        try {
            this.defaultInstance = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No no-arg constructor for " + clazz.getName(), e);
        }
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        try {
            for (Field field : src.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(src);
                Object def = field.get(defaultInstance);
                if (value == null) continue;
                if (jsonEqual(value, def)) continue;
                out.add(jsonName(field), context.serialize(value));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    private static String jsonName(Field field) {
        SerializedName sn = field.getAnnotation(SerializedName.class);
        return sn != null ? sn.value() : field.getName();
    }

    private static boolean jsonEqual(Object a, Object b) {
        return CHECKER.toJson(a).equals(CHECKER.toJson(b));
    }
}
