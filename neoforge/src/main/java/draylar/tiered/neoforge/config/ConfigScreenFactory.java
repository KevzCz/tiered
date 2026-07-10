package draylar.tiered.neoforge.config;

import draylar.tiered.config.TieredConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * NeoForge has no ModMenu equivalent baked in - it exposes its own native config-screen
 * extension point instead. Wires up the same Cloth-Config-generated screen
 * ({@link TieredConfig} via {@code AutoConfig}) that Fabric exposes through ModMenu, so the
 * config content stays identical on both loaders; only the entry point differs.
 */
public final class ConfigScreenFactory implements IConfigScreenFactory {

    private ConfigScreenFactory() {
    }

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, new ConfigScreenFactory());
    }

    @Override
    public Screen createScreen(ModContainer container, Screen parent) {
        return AutoConfig.getConfigScreen(TieredConfig.class, parent).get();
    }
}
