package draylar.tiered;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.LinkedList;
import java.util.Queue;

@Environment(EnvType.SERVER)
public class TieredServer implements DedicatedServerModInitializer {

    public static final Queue<Runnable> TASK_QUEUE = new LinkedList<>();

    @Override
    public void onInitializeServer() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            while (!TASK_QUEUE.isEmpty()) {
                TASK_QUEUE.poll().run();
            }
        });
    }
}
