package draylar.tiered.api.neoforge;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class CooldownsImpl {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "tiered");

    private static final Codec<Map<String, Integer>> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    public static final Supplier<AttachmentType<Map<String, Integer>>> DATA = ATTACHMENT_TYPES.register(
            "cooldowns",
            () -> AttachmentType.<Map<String, Integer>>builder((holder) -> new HashMap<>())
                    .serialize(CODEC)
                    .build());

    private CooldownsImpl() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static void start(Player player, String id, int ticks) {
        if (ticks <= 0) { clear(player, id); return; }
        Map<String, Integer> map = new HashMap<>(player.getData(DATA));
        map.put(id, ticks);
        player.setData(DATA, map);
    }

    public static int remaining(Player player, String id) {
        Integer r = player.getData(DATA).get(id);
        return r == null ? 0 : Math.max(0, r);
    }

    public static void clear(Player player, String id) {
        Map<String, Integer> existing = player.getData(DATA);
        if (!existing.containsKey(id)) return;
        Map<String, Integer> map = new HashMap<>(existing);
        map.remove(id);
        player.setData(DATA, map);
    }

    public static void tick(Player player) {
        Map<String, Integer> current = player.getData(DATA);
        if (current.isEmpty()) return;
        Map<String, Integer> next = new HashMap<>();
        for (Map.Entry<String, Integer> e : current.entrySet()) {
            int r = e.getValue() - 1;
            if (r > 0) next.put(e.getKey(), r);
        }
        player.setData(DATA, next);
    }
}
