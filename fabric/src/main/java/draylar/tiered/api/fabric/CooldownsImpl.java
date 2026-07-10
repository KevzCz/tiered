package draylar.tiered.api.fabric;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import draylar.tiered.Tiered;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

public final class CooldownsImpl {

    public static final AttachmentType<Map<String, Integer>> DATA = AttachmentRegistry.create(
            Tiered.id("cooldowns"),
            builder -> builder
                    .persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .initializer(HashMap::new));

    private CooldownsImpl() {
    }

    public static void start(Player player, String id, int ticks) {
        if (ticks <= 0) { clear(player, id); return; }
        Map<String, Integer> map = new HashMap<>(player.getAttachedOrCreate(DATA));
        map.put(id, ticks);
        player.setAttached(DATA, map);
    }

    public static int remaining(Player player, String id) {
        Integer r = player.getAttachedOrCreate(DATA).get(id);
        return r == null ? 0 : Math.max(0, r);
    }

    public static void clear(Player player, String id) {
        Map<String, Integer> existing = player.getAttachedOrCreate(DATA);
        if (!existing.containsKey(id)) return;
        Map<String, Integer> map = new HashMap<>(existing);
        map.remove(id);
        player.setAttached(DATA, map);
    }

    public static void tick(Player player) {
        Map<String, Integer> current = player.getAttachedOrCreate(DATA);
        if (current.isEmpty()) return;
        Map<String, Integer> next = new HashMap<>();
        for (Map.Entry<String, Integer> e : current.entrySet()) {
            int r = e.getValue() - 1;
            if (r > 0) next.put(e.getKey(), r);
        }
        player.setAttached(DATA, next);
    }
}
