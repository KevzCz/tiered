package draylar.tiered.api;

import java.util.List;

public interface SpawnStructureHolder {

    List<String> tiered$getSpawnStructureIds();

    List<String> tiered$getSpawnStructureTags();

    void tiered$setSpawnStructures(List<String> ids, List<String> tags);
}
