package net.diexv.potionenchant.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * 服务端延迟任务调度（按目标服务端 tick 排队，每 tick 检查到期执行）。
 * 参考 DiexvSword V6 的 scheduleTickTask；注意 server.tell(TickTask) 并不会延迟（run 直接执行），
 * 因此延后动作必须用本调度器。
 */
@EventBusSubscriber(modid = PotionEnchantMod.MODID, bus = Bus.FORGE)
public final class TickTaskScheduler {

    private static final TreeMap<Long, List<Runnable>> TASKS = new TreeMap<>();

    private TickTaskScheduler() {
    }

    /** 在 delayTicks 个服务端 tick 后执行（服务端暂停时自然顺延） */
    public static void schedule(MinecraftServer server, long delayTicks, Runnable task) {
        if (server == null) return;
        long at = server.getTickCount() + Math.max(1L, delayTicks);
        synchronized (TASKS) {
            TASKS.computeIfAbsent(at, k -> new ArrayList<>()).add(task);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;
        List<Runnable> due = new ArrayList<>();
        synchronized (TASKS) {
            long now = server.getTickCount();
            Iterator<Map.Entry<Long, List<Runnable>>> it = TASKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, List<Runnable>> e = it.next();
                if (e.getKey() > now) break;
                due.addAll(e.getValue());
                it.remove();
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
