package net.diexv.potionenchant.util;

import net.diexv.potionenchant.agent.DiexvSwordAgent;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * 主模组 → agent 的桥。
 *
 * agent 由 PotionEnchantMixinPlugin.onLoad 经 loadAgent0（AgentLauncher）启动；
 * 注入代码引用的 DiexvSwordZeroState 由 AgentLauncher 用 DiexvBase.defineClassInPackage
 * 定义到 SystemClassLoader（纯 Java，兼容手机启动器），因此 SynchedEntityData
 * （Platform 委托链）解析到的是 system 那份。
 *
 * 本桥的 mark/unmark 必须也调用 system 那份（与注入代码共享同一静态目标集合），
 * 不能直接调用主模组加载的同名类（那是另一份静态状态，会归零失效）。
 */
public final class DiexvSwordAgentBridge {

    private static final String ZERO_STATE = "net.diexv.potionenchant.agent.DiexvSwordZeroState";

    private DiexvSwordAgentBridge() {
    }

    /** 取 SystemClassLoader 里的 helper（AgentLauncher define 的那份）；未就绪返回 null */
    private static Class<?> systemZeroState() {
        try {
            return Class.forName(ZERO_STATE, true, ClassLoader.getSystemClassLoader());
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 取 **mod jar 内**的那份 helper。
     *
     * 历史堆栈已证实：注入到 net.minecraft 类里的字节码解析到的是 mod jar 内的
     * DiexvSwordZeroState（~[potionenchant-....jar%23xxx!/]），与 AgentLauncher 用
     * defineClass 在 SystemClassLoader 里另造的那一份**不是同一个类**（静态字段不通）。
     * 因此这里两份都要操作，确保注入代码读到正确的目标集合。
     */
    private static Class<?> modZeroState() {
        try {
            return net.diexv.potionenchant.agent.DiexvSwordZeroState.class;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 对两份 helper 都执行同一个静态方法调用（幂等） */
    private static void invokeOnBoth(String method, Class<?> argType, Object arg) {
        Class<?> mod = modZeroState();
        if (mod != null) {
            try {
                mod.getMethod(method, argType).invoke(null, arg);
            } catch (Throwable ignored) {
            }
        }
        Class<?> sys = systemZeroState();
        if (sys != null && sys != mod) {
            try {
                sys.getMethod(method, argType).invoke(null, arg);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * redefine 触发点：由剑首次标记目标时调用（DiexvSwordTargetZeroManager.mark）。
     * 确保 SynchedEntityData 已注入（已加载时 retransform）。
     */
    public static void ensureRedefined() {
        DiexvSwordAgent.ensureRedefined();
    }

    /** 把目标实体标记进 agent 的归零集合（mod 副本 + system 副本都写，见 modZeroState 注释；静默不打印日志） */
    public static void markTarget(Entity entity) {
        if (entity == null) return;
        invokeOnBoth("add", UUID.class, entity.getUUID());
    }

    /** 取消标记（两份都清） */
    public static void unmarkTarget(Entity entity) {
        if (entity == null) return;
        invokeOnBoth("remove", UUID.class, entity.getUUID());
    }
}
