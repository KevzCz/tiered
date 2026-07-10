package draylar.tiered.registry;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public final class StructureContext {

    public record Result(List<String> ids, List<String> tags) {
        public static final Result EMPTY = new Result(List.of(), List.of());
    }

    private StructureContext() {}

    public static Result resolve(ServerLevel world, BlockPos pos) {
        if (world == null || pos == null) return Result.EMPTY;
        StructureManager accessor = world.structureManager();
        Map<Structure, LongSet> references = accessor.getAllStructuresAt(pos);
        if (references.isEmpty()) return Result.EMPTY;

        Registry<Structure> reg = world.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<String> ids = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (Structure structure : references.keySet()) {
            StructureStart start = accessor.getStructureWithPieceAt(pos, structure);
            if (start == null || !start.isValid()) continue;
            Holder<Structure> entry = reg.getHolder(reg.getId(structure)).orElse(null);
            if (entry == null) continue;
            entry.unwrapKey().ifPresent(key -> ids.add(key.location().toString()));
            entry.tags().forEach(tag -> {
                String id = tag.location().toString();
                if (!tags.contains(id)) tags.add(id);
            });
        }
        return new Result(ids, tags);
    }
}
