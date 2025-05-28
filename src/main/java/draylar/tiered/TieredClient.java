package draylar.tiered;

import draylar.tiered.api.BorderTemplate;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.data.TooltipBorderLoader;
import draylar.tiered.network.TieredClientPacket;
import draylar.tiered.reforge.ReforgeScreen;
import draylar.tiered.reforge.ReforgeScreenHandler;
import draylar.tiered.reforge.widget.AnvilTab;
import draylar.tiered.reforge.widget.ReforgeTab;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.libz.registry.TabRegistry;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

@Environment(EnvType.CLIENT)
public class TieredClient implements ClientModInitializer {

    // map for storing attributes before logging into a server
    public static final Map<Identifier, PotentialAttribute> CACHED_ATTRIBUTES = new HashMap<>();

    public static final List<BorderTemplate> BORDER_TEMPLATES = new ArrayList<BorderTemplate>();

    private static final Identifier ANVIL_TAB_ICON = Identifier.of("tiered:textures/gui/anvil_tab_icon.png");
    private static final Identifier REFORGE_TAB_ICON = Identifier.of("tiered:textures/gui/reforge_tab_icon.png");
    public static final Queue<Runnable> TASK_QUEUE = new LinkedList<>();
    @Override
    public void onInitializeClient() {
        HandledScreens.<ReforgeScreenHandler, ReforgeScreen>register(Tiered.REFORGE_SCREEN_HANDLER_TYPE, ReforgeScreen::new);
        TieredClientPacket.init();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new TooltipBorderLoader());
        TabRegistry.registerOtherTab(new AnvilTab(Text.translatable("container.repair"), ANVIL_TAB_ICON, 0, AnvilScreen.class), AnvilScreen.class);
        TabRegistry.registerOtherTab(new ReforgeTab(Text.translatable("screen.tiered.reforging_screen"), REFORGE_TAB_ICON, 1, ReforgeScreen.class), AnvilScreen.class);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (!TASK_QUEUE.isEmpty()) {
                TASK_QUEUE.poll().run();
            }
        });
    }

}
