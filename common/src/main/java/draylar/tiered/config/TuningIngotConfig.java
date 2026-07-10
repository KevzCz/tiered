package draylar.tiered.config;

public class TuningIngotConfig {
    public String group;
    public String color;
    public float lootChance;
    public int minCount;
    public int maxCount;

    public TuningIngotConfig() {}

    public TuningIngotConfig(String group, String color, float lootChance, int minCount, int maxCount) {
        this.group = group;
        this.color = color;
        this.lootChance = lootChance;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }
}
