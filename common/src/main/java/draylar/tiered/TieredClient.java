package draylar.tiered;

import dev.architectury.platform.Platform;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import draylar.tiered.api.BorderTemplate;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.data.TooltipBorderLoader;
import draylar.tiered.util.ReforgeMaterialTooltip;
import draylar.tiered.util.TieredNeoForgeStyleTooltip;
import draylar.tiered.lib.TabRegistry;
import draylar.tiered.network.TieredClientPacket;
import draylar.tiered.reforge.ReforgeScreen;
import draylar.tiered.reforge.widget.AnvilTab;
import draylar.tiered.reforge.widget.ReforgeTab;
import draylar.tiered.util.ReforgeMaterials;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import java.util.*;

@Environment(EnvType.CLIENT)
public class TieredClient {

    public static final Map<ResourceLocation, PotentialAttribute> CACHED_ATTRIBUTES = new HashMap<>();

    public static final List<BorderTemplate> BORDER_TEMPLATES = new ArrayList<BorderTemplate>();
    private static final ResourceLocation ANVIL_TAB_ICON = ResourceLocation.parse("tiered:textures/gui/anvil_tab_icon.png");
    private static final ResourceLocation REFORGE_TAB_ICON = ResourceLocation.parse("tiered:textures/gui/reforge_tab_icon.png");
    public static final Queue<Runnable> TASK_QUEUE = new LinkedList<>();

    public static void init() {
        TieredKeybinds.register();
        TieredClientPacket.init();
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new TooltipBorderLoader());
        TabRegistry.registerOtherTab(new AnvilTab(Component.translatable("container.repair"), ANVIL_TAB_ICON, 0, AnvilScreen.class), AnvilScreen.class);
        TabRegistry.registerOtherTab(new ReforgeTab(Component.translatable("screen.tiered.reforging_screen"), REFORGE_TAB_ICON, 1, ReforgeScreen.class), AnvilScreen.class);
        ClientTickEvent.CLIENT_POST.register(client -> {
            while (!TASK_QUEUE.isEmpty()) {
                TASK_QUEUE.poll().run();
            }
        });

        ClientTooltipEvent.ITEM.register((stack, lines, tooltipContext, flag) -> {
            ReforgeMaterial material = ReforgeMaterials.resolve(stack);
            ReforgeMaterialTooltip.appendAll(lines, stack, material,
                    Minecraft.getInstance().player);
            TieredNeoForgeStyleTooltip.reformat(lines, stack);
            if (Platform.isFabric()) TieredNeoForgeStyleTooltip.appendAccessoryOnlyLines(lines, stack);
        });
    }

}
