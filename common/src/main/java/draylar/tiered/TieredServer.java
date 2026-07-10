package draylar.tiered;

import dev.architectury.event.events.common.TickEvent;

import java.util.LinkedList;
import java.util.Queue;

public class TieredServer {

    public static final Queue<Runnable> TASK_QUEUE = new LinkedList<>();

    public static void init() {
        TickEvent.SERVER_POST.register(server -> {
            while (!TASK_QUEUE.isEmpty()) {
                TASK_QUEUE.poll().run();
            }
        });
    }
}
