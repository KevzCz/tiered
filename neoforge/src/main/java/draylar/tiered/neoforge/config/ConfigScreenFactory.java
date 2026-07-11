package draylar.tiered.neoforge.config;

import draylar.tiered.config.TieredConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

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
