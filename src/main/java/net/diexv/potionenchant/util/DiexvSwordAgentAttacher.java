package net.diexv.potionenchant.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Attach agent 动态附加器：由 PotionEnchantMixinPlugin.onLoad 调用。
 *
 * 流程：
 *  1. 从主 jar 提取内置 agent（META-INF/potionenchant/potionenchant-agent.jar）到游戏目录
 *  2. 启动独立 JVM 运行 AttachHelper，从外部 attach 本 JVM 并 loadAgent
 *     （外部 attach 不受 jdk.attach.allowAttachSelf 限制）
 *  3. 失败时兜底 self-attach（需 -Djdk.attach.allowAttachSelf=true）
 *
 * agent（DiexvSwordAgent）注入字节码 redefine：
 *  SynchedEntityData.set/get 与 LivingEntity.setHealth/getHealth 归 0（由 diexvsword 触发）。
 */
public final class DiexvSwordAgentAttacher {

    private static final String AGENT_RESOURCE = "/META-INF/potionenchant/potionenchant-agent.jar";
    private static final String HELPER_CLASS = "net.diexv.potionenchant.agent.AttachHelper";
    private static final long HELPER_TIMEOUT_SECONDS = 30L;

    private static volatile boolean attached;

    private DiexvSwordAgentAttacher() {}

    public static void attach() {
        if (attached) return;
        synchronized (DiexvSwordAgentAttacher.class) {
            if (attached) return;
            try {
                File agentJar = extractAgentJar();
                if (agentJar == null) {
                    log("未找到内置 agent jar: " + AGENT_RESOURCE);
                    return;
                }
                String agentPath = agentJar.getAbsolutePath();
                if (launchExternalAttach(agentPath)) {
                    log("外部附加成功: " + agentPath);
                    attached = true;
                    return;
                }
                log("外部附加失败，尝试 self-attach 兜底");
                if (attachSelf(agentPath)) {
                    log("self-attach 兜底成功");
                    attached = true;
                    return;
                }
                log("动态附加失败：外部 attach 与 self-attach 均不可用");
            } catch (Throwable t) {
                log("javaagent 动态附加失败: " + t);
            }
        }
    }

    /** 启动独立 JVM 运行 AttachHelper（外部 attach，不受 allowAttachSelf 限制） */
    private static boolean launchExternalAttach(String agentPath) {
        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            command.add(javaExecutable());
            command.add("-cp");
            command.add(agentPath);
            command.add(HELPER_CLASS);
            command.add(String.valueOf(ProcessHandle.current().pid()));
            command.add(agentPath);
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(HELPER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log("外部 helper 超时（" + HELPER_TIMEOUT_SECONDS + "s）");
                return false;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (!output.isEmpty()) {
                log("helper 输出: " + output);
            }
            return process.exitValue() == 0;
        } catch (Throwable t) {
            log("外部 attach 启动失败: " + t);
            if (process != null) process.destroyForcibly();
            return false;
        }
    }

    /** self-attach 兜底；仅当 JVM 带 -Djdk.attach.allowAttachSelf=true 启动时可用 */
    private static boolean attachSelf(String agentPath) {
        try {
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Object vm = vmClass.getMethod("attach", String.class)
                    .invoke(null, String.valueOf(ProcessHandle.current().pid()));
            try {
                vmClass.getMethod("loadAgent", String.class).invoke(vm, agentPath);
                return true;
            } finally {
                try {
                    vmClass.getMethod("detach").invoke(vm);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            log("self-attach 失败: " + t);
            return false;
        }
    }

    private static File extractAgentJar() throws IOException {
        Path out = resolveOutput();
        // 1) 从主 jar 文件直读（与 DiexvSword 一致，优先使用 JarFile，兼容 ModuleClassLoader）
        File mainJar = findMainJarPath();
        if (mainJar != null && mainJar.isFile()) {
            try (java.util.jar.JarFile jf = new java.util.jar.JarFile(mainJar)) {
                java.util.jar.JarEntry entry = jf.getJarEntry("META-INF/potionenchant/potionenchant-agent.jar");
                if (entry != null) {
                    Files.createDirectories(out.getParent());
                    try (InputStream in = jf.getInputStream(entry)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                    log("已从主 jar 直读内置 agent: " + out.toAbsolutePath());
                    return out.toFile();
                }
            }
        }
        // 2) classpath 资源兜底（getResourceAsStream 可能因 ModuleClassLoader 而返回无效数据）
        try (InputStream in = DiexvSwordAgentAttacher.class.getResourceAsStream(AGENT_RESOURCE)) {
            if (in != null) {
                Files.createDirectories(out.getParent());
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                return out.toFile();
            }
        }
        log("主 jar 内无内置 agent 条目 (META-INF/potionenchant/potionenchant-agent.jar)");
        return null;
    }

    /** 从 code source 解析主 jar 真实路径（兼容 ModLauncher 的 union: 协议和 ModuleClassLoader 的 jar: 协议） */
    private static File findMainJarPath() {
        try {
            String location = DiexvSwordAgentAttacher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().toString();
            // ModuleClassLoader（SecureJarClassLoader）返回 jar:file:///path!/ 格式，
            // 而 SERVICE 层返回 file:///path 格式。先统一剥离 jar: 前缀。
            if (location.startsWith("jar:")) {
                location = location.substring(4);
            }
            if (location.startsWith("union:")) {
                int idx = location.indexOf("file:");
                location = idx != -1 ? location.substring(idx) : location.substring(6);
            }
            if (location.startsWith("file:")) {
                location = location.substring(5);
            }
            location = java.net.URLDecoder.decode(location, StandardCharsets.UTF_8);
            int bang = location.indexOf('!');
            int hash = location.indexOf('#');
            int cut = -1;
            if (bang != -1 && hash != -1) cut = Math.min(bang, hash);
            else if (bang != -1) cut = bang;
            else if (hash != -1) cut = hash;
            if (cut != -1) location = location.substring(0, cut);
            File file = new File(location);
            if (file.isFile() && location.endsWith(".jar")) return file;
            log("主 jar 路径无效: " + location);
        } catch (Throwable t) {
            log("解析主 jar 路径失败: " + t);
        }
        return null;
    }

    /** 优先游戏目录（user.dir）根目录，不可用时回退临时目录 */
    private static Path resolveOutput() {
        try {
            Path gameDir = Path.of(System.getProperty("user.dir", "."));
            if (Files.isWritable(gameDir)) {
                return gameDir.resolve("potionenchant-agent.jar");
            }
        } catch (Throwable ignored) {
        }
        return Path.of(System.getProperty("java.io.tmpdir", "."), "potionenchant", "potionenchant-agent.jar");
    }

    private static String javaExecutable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            String fileName = isWindows() ? "java.exe" : "java";
            File candidate = new File(new File(javaHome, "bin"), fileName);
            if (candidate.isFile()) return candidate.getAbsolutePath();
        }
        return isWindows() ? "java.exe" : "java";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void log(String msg) {
        System.out.println("[DiexvSwordAgent] " + msg);
    }
}