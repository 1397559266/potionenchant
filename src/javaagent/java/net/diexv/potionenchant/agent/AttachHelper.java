package net.diexv.potionenchant.agent;

/**
 * 外部附加辅助进程：由主模组以独立 JVM 启动，
 * 从外部 attach 目标游戏 JVM 并加载 agent。
 *
 * 用法：
 *   java -cp potionenchant-agent.jar net.diexv.potionenchant.agent.AttachHelper <targetPid> <agentJar>
 *
 * 外部 attach 不受 jdk.attach.allowAttachSelf 限制，因此目标 JVM 无需任何附加参数。
 */
public final class AttachHelper {

    private AttachHelper() {}

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("[DiexvSwordAgent] AttachHelper 用法: <targetPid> <agentJar>");
            System.exit(2);
        }
        String targetPid = args[0];
        String agentJar = args[1];

        boolean loaded = false;
        try {
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Object vm = vmClass.getMethod("attach", String.class).invoke(null, targetPid);
            try {
                vmClass.getMethod("loadAgent", String.class).invoke(vm, agentJar);
                System.out.println("[DiexvSwordAgent] 外部附加成功: pid=" + targetPid + " agent=" + agentJar);
                loaded = true;
            } finally {
                try {
                    vmClass.getMethod("detach").invoke(vm);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            System.err.println("[DiexvSwordAgent] 外部附加失败: " + t);
        }
        System.exit(loaded ? 0 : 1);
    }
}
