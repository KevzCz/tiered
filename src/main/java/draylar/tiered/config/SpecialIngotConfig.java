package draylar.tiered.config;

import java.util.List;

public class SpecialIngotConfig {
    public float totalSpecialPercent = 0.5f;
    public float dropChance = 0.01f;
    public List<String> lootTables;
    public List<SpecialStatEntry> specialStats;
    public List<BasicStatEntry> basicStats;
    public List<String> blockedItemIds;
    public List<String> blockedItemTags;

    public SpecialIngotConfig() {}
}
