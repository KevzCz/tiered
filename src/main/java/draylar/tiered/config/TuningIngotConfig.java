package draylar.tiered.config;

public class TuningIngotConfig {
    public String group;
    public String color;
    public float lootChance; // between 0.0 and 1.0

    public TuningIngotConfig() {}

    public TuningIngotConfig(String group, String color, float lootChance) {
        this.group = group;
        this.color = color;
        this.lootChance = lootChance;
    }
}
