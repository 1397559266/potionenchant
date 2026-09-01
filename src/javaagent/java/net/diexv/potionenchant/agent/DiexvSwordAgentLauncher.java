package net.diexv.potionenchant.agent;

/**
 * JavaAgent 入口（manifest 指向此类）——两段式加载，参考项目 DiexvAgentLauncher。
 *
 * 为什么需要两段式：
 *  - Forge 1.20.1 的 mod 类由 cpw.mods.cl.ModuleClassLoader 加载，其未命中父层模块时的
 *    兜底加载器是 PlatformClassLoader（不是 AppClassLoader）。动态 attach 时自动追加到
 *    "系统 classpath" 的 agent jar 对 mod 类不可见；而注入到 SynchedEntityData/LivingEntity
 *    （bootstrap/Platform 加载）里的字节码引用 agent 类，必须让这些类对 bootstrap 可见。
 *  - 修复：把 agent jar 追加到 bootstrap 类路径，再从 bootstrap 反射加载核心 agent
 *    （DiexvSwordAgent），保证核心 agent 的所有类都来自同一个 bootstrap 加载器。
 *
 * 本类放在独立包由 App 加载，不直接引用核心 agent 类。
 */
public final class DiexvSwordAgentLauncher {

    private DiexvSwordAgentLauncher() {}

    public static void premain(String agentArgs, java.lang.instrument.Instrumentation inst) {
        start(inst);
    }

    public static void agentmain(String agentArgs, java.lang.instrument.Instrumentation inst) {
        start(inst);
    }

    private static void start(java.lang.instrument.Instrumentation inst) {
        try {
            java.io.File jar = locateAgentJar();
            java.util.jar.JarFile jf = new java.util.jar.JarFile(jar);
            inst.appendToBootstrapClassLoaderSearch(jf);
            inst.appendToSystemClassLoaderSearch(jf);
            System.out.println("[DiexvSwordAgent] agent jar 已追加到 bootstrap+system 类路径: " + jar.getAbsolutePath());
            Class<?> core = Class.forName("net.diexv.potionenchant.agent.DiexvSwordAgent", true, null);
            if (core.getClassLoader() != null) {
                throw new IllegalStateException("核心 agent 未从 bootstrap 加载: loader=" + core.getClassLoader());
            }
            core.getMethod("install", java.lang.instrument.Instrumentation.class).invoke(null, inst);
        } catch (Throwable t) {
            System.err.println("[DiexvSwordAgent] launcher 启动失败: " + t);
            t.printStackTrace();
        }
    }

    private static java.io.File locateAgentJar() throws Exception {
        java.net.URL loc = DiexvSwordAgentLauncher.class.getProtectionDomain().getCodeSource().getLocation();
        if (loc == null) {
            throw new IllegalStateException("无法定位 agent jar");
        }
        java.io.File f = new java.io.File(loc.toURI());
        if (!f.isFile() || !f.getName().endsWith(".jar")) {
            throw new IllegalStateException("agent 位置不是 jar: " + f);
        }
        return f;
    }
}
