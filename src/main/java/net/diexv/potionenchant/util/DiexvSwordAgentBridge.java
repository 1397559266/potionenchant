package net.diexv.potionenchant.util;

import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * 主模组 → agent 的反射桥（参考项目 EntityAuthorityBridge）。
 *
 * agent（DiexvSwordZeroState）由 Launcher 追加到 bootstrap 类路径，主模组类
 * （ModuleClassLoader，委托链可达 bootstrap）可通过反射访问它；
 * 反向（agent 注入的字节码）只引用 agent 自包含类，不依赖主模组。
 */
public final class DiexvSwordAgentBridge {

    private static final String ZERO_STATE = "net.diexv.potionenchant.agent.DiexvSwordZeroState";
    private static final String AGENT_CLASS = "net.diexv.potionenchant.agent.DiexvSwordAgent";

    private DiexvSwordAgentBridge() {}

    /**
     * redefine 触发点：由剑首次标记目标时调用（DiexvSwordTargetZeroManager.mark）。
     * 对已加载的目标类执行 retransformOrRedefine（V6 模式，见 DiexvSwordAgent）。
     */
    public static void ensureRedefined() {
        try {
            Class<?> c = Class.forName(AGENT_CLASS, true, DiexvSwordAgentBridge.class.getClassLoader());
            c.getMethod("ensureRedefined").invoke(null);
        } catch (Throwable t) {
            System.err.println("[DiexvSwordAgentBridge] ensureRedefined 失败: " + t);
        }
    }

    /** 把目标实体标记进 agent 的归零集合（agent 注入的百分比伤害 redefine 据此把血量归 0） */
    public static void markTarget(Entity entity) {
        if (entity == null) return;
        try {
            Class<?> c = Class.forName(ZERO_STATE, true, DiexvSwordAgentBridge.class.getClassLoader());
            c.getMethod("add", UUID.class).invoke(null, entity.getUUID());
            System.out.println("[DiexvSwordAgentBridge] markTarget 成功: " + entity.getUUID());
        } catch (Throwable t) {
            System.err.println("[DiexvSwordAgentBridge] markTarget 失败: " + t);
            t.printStackTrace(System.err);
        }
    }

    /** 取消标记 */
    public static void unmarkTarget(Entity entity) {
        if (entity == null) return;
        try {
            Class<?> c = Class.forName(ZERO_STATE, true, DiexvSwordAgentBridge.class.getClassLoader());
            c.getMethod("remove", UUID.class).invoke(null, entity.getUUID());
        } catch (Throwable ignored) {
        }
    }
}