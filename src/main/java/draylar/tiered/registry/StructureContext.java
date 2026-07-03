package draylar.tiered.registry;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StructureContext {

    public record Result(List<String> ids, List<String> tags) {
        public static final Result EMPTY = new Result(List.of(), List.of());
    }

    private StructureContext() {}

    public static Result resolve(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return Result.EMPTY;
        StructureAccessor accessor = world.getStructureAccessor();
        Map<Structure, LongSet> references = accessor.getStructureReferences(pos);
        if (references.isEmpty()) return Result.EMPTY;

        Registry<Structure> reg = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        List<String> ids = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (Structure structure : references.keySet()) {
            StructureStart start = accessor.getStructureContaining(pos, structure);
            if (start == null || !start.hasChildren()) continue;
            RegistryEntry<Structure> entry = reg.getEntry(reg.getRawId(structure)).orElse(null);
            if (entry == null) continue;
            entry.getKey().ifPresent(key -> ids.add(key.getValue().toString()));
            entry.streamTags().forEach(tag -> {
                String id = tag.id().toString();
                if (!tags.contains(id)) tags.add(id);
            });
        }
        return new Result(ids, tags);
    }
}
