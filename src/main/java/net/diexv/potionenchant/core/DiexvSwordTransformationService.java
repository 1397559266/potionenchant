package net.diexv.potionenchant.core;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.ModuleLayerHandler;
import cpw.mods.modlauncher.api.*;
import net.minecraftforge.fml.loading.ModDirTransformerDiscoverer;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ITransformationService（coremod）——在 ModLauncher SERVICE 阶段（系统类加载器）触发。
 *
 * 与 DiexvSword 的 DiexvTransformationService 一致，完整移植：
 * 1. 内置 javaagent 动态附加（外部 attach + self-attach 兜底）
 * 2. coremod 与 mod 层共存逻辑（coexistenceCoreAndMod）
 */
public final class DiexvSwordTransformationService implements ITransformationService {

    // ===== 反射工具 =====
    private static final Unsafe U;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            U = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static long objectFieldOffset(Field f) {
        try {
            return U.objectFieldOffset(f);
        } catch (UnsupportedOperationException e) {
            return -1L;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object target, String fieldName, Class<T> clazz) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            long offset;
            if (Modifier.isStatic(f.getModifiers())) {
                target = U.staticFieldBase(f);
                offset = U.staticFieldOffset(f);
            } else {
                offset = objectFieldOffset(f);
            }
            if (offset >= 0) {
                return (T) U.getObject(target, offset);
            }
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Class<?> target, String fieldName, Class<T> clazz) {
        try {
            Field f = target.getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            if (Modifier.isStatic(f.getModifiers())) {
                long offset = U.staticFieldOffset(f);
                U.putObject(U.staticFieldBase(f), offset, value);
            } else {
                long offset = objectFieldOffset(f);
                if (offset >= 0) {
                    U.putObject(target, offset, value);
                } else {
                    f.setAccessible(true);
                    f.set(target, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getJarPath(Class<?> clazz) {
        String file = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (!file.isEmpty()) {
            if (file.startsWith("union:"))
                file = file.substring(6);
            if (file.startsWith("/"))
                file = file.substring(1);
            file = file.substring(0, file.lastIndexOf(".jar") + 4);
            file = file.replaceAll("/", "\\\\");
        }
        return URLDecoder.decode(file, StandardCharsets.UTF_8);
    }

    // ===== 共存逻辑 =====
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void coexistenceCoreAndMod() {
        List<cpw.mods.modlauncher.api.NamedPath> found = getFieldValue(ModDirTransformerDiscoverer.class, "found", List.class);
        found.removeIf(np -> getJarPath(DiexvSwordTransformationService.class).equals(np.paths()[0].toString()));

        getFieldValue(getFieldValue(Launcher.INSTANCE, "moduleLayerHandler", ModuleLayerHandler.class), "completedLayers", EnumMap.class).values().forEach(layerInfo -> {
            ModuleLayer layer = getFieldValue(layerInfo, "layer", ModuleLayer.class);
            layer.modules().forEach(module -> {
                if (module.getName().equals(DiexvSwordTransformationService.class.getModule().getName())) {
                    Set<ResolvedModule> modules = new HashSet<>(getFieldValue(layer.configuration(), "modules", Set.class));
                    Map<String, ResolvedModule> nameToModule = new HashMap(getFieldValue(layer.configuration(), "nameToModule", Map.class));
                    modules.remove(nameToModule.remove(DiexvSwordTransformationService.class.getModule().getName()));
                    setFieldValue(layer.configuration(), "modules", modules);
                    setFieldValue(layer.configuration(), "nameToModule", nameToModule);
                }
            });
        });
    }

    // ===== ITransformationService =====
    @Override
    public String name() {
        return "potionenchant_agent";
    }

    @Override
    public void initialize(IEnvironment environment) {
        Path gameDir = null;
        try {
            if (environment != null) {
                gameDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElse(null);
            }
        } catch (Throwable ignored) {
        }
        attachBuiltinAgent(gameDir);
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        coexistenceCoreAndMod();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<ITransformer> transformers() {
        return List.of();
    }

    // ===== 内置 javaagent 动态附加 =====

    private static final String AGENT_RESOURCE = "/META-INF/potionenchant/potionenchant-agent.jar";
    private static final String HELPER_CLASS = "net.diexv.potionenchant.agent.AttachHelper";
    private static final long HELPER_TIMEOUT_SECONDS = 30;
    private static volatile boolean agentAttached = false;

    private static void attachBuiltinAgent(Path gameDir) {
        if (agentAttached) {
            return;
        }
        synchronized (DiexvSwordTransformationService.class) {
            if (agentAttached) {
                return;
            }
            try {
                File agentJar = extractBuiltinAgentJar(gameDir);
                if (agentJar == null) {
                    System.err.println("[PotionEnchantAgent] 未找到内置 agent jar: " + AGENT_RESOURCE);
                    return;
                }
                String agentPath = agentJar.getAbsolutePath();
                if (launchExternalAttach(agentPath)) {
                    System.out.println("[PotionEnchantAgent] 外部附加成功");
                    agentAttached = true;
                    return;
                }
                System.err.println("[PotionEnchantAgent] 外部附加失败，尝试 self-attach 兜底");
                if (attachSelf(agentPath)) {
                    System.out.println("[PotionEnchantAgent] self-attach 兜底成功");
                    agentAttached = true;
                } else {
                    System.err.println("[PotionEnchantAgent] 动态附加失败：外部 attach 与 self-attach 均不可用，本次运行不注入 agent");
                }
            } catch (Throwable t) {
                System.err.println("[PotionEnchantAgent] javaagent 动态附加失败: " + t);
            }
        }
    }

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
                System.err.println("[PotionEnchantAgent] 外部 helper 超时（" + HELPER_TIMEOUT_SECONDS + "s）");
                return false;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (!output.isEmpty()) {
                System.out.println("[PotionEnchantAgent] helper 输出: " + output);
            }
            return process.exitValue() == 0;
        } catch (Throwable t) {
            System.err.println("[PotionEnchantAgent] 外部 attach 启动失败: " + t);
            if (process != null) {
                process.destroyForcibly();
            }
            return false;
        }
    }

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
            System.err.println("[PotionEnchantAgent] self-attach 失败: " + t);
            return false;
        }
    }

    private static String javaExecutable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            String fileName = isWindows() ? "java.exe" : "java";
            File candidate = new File(new File(javaHome, "bin"), fileName);
            if (candidate.isFile()) {
                return candidate.getAbsolutePath();
            }
        }
        return isWindows() ? "java.exe" : "java";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static File resolveAgentOutput(Path gameDir) {
        if (gameDir != null) {
            try {
                Files.createDirectories(gameDir);
                if (Files.isWritable(gameDir)) {
                    return gameDir.resolve("potionenchant-agent.jar").toFile();
                }
            } catch (IOException ignored) {
            }
        }
        return new File(new File(System.getProperty("java.io.tmpdir"), "potionenchant"), "potionenchant-agent.jar");
    }

    private static File extractBuiltinAgentJar(Path gameDir) throws IOException {
        try (InputStream in = DiexvSwordTransformationService.class.getResourceAsStream(AGENT_RESOURCE)) {
            if (in != null) {
                File out = resolveAgentOutput(gameDir);
                Files.createDirectories(out.getParentFile().toPath());
                Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return out;
            }
        }
        File mainJar = findMainJarPath();
        if (mainJar != null && mainJar.isFile()) {
            try (JarFile jf = new JarFile(mainJar)) {
                JarEntry entry = jf.getJarEntry("META-INF/potionenchant/potionenchant-agent.jar");
                if (entry != null) {
                    File out = resolveAgentOutput(gameDir);
                    Files.createDirectories(out.getParentFile().toPath());
                    try (InputStream in = jf.getInputStream(entry)) {
                        Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    System.out.println("[PotionEnchantAgent] 已从主 jar 提取内置 agent: " + out.getAbsolutePath());
                    return out;
                }
            }
        }
        return null;
    }

    private static File findMainJarPath() {
        try {
            String location = DiexvSwordTransformationService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().toString();
            if (location.startsWith("union:")) {
                int idx = location.indexOf("file:");
                location = idx != -1 ? location.substring(idx) : location.substring(6);
            }
            if (location.startsWith("file:")) {
                location = location.substring(5);
            }
            location = URLDecoder.decode(location, StandardCharsets.UTF_8);
            int bang = location.indexOf('!');
            int hash = location.indexOf('#');
            int cut = -1;
            if (bang != -1 && hash != -1) {
                cut = Math.min(bang, hash);
            } else if (bang != -1) {
                cut = bang;
            } else if (hash != -1) {
                cut = hash;
            }
            if (cut != -1) {
                location = location.substring(0, cut);
            }
            File file = new File(location);
            if (file.isFile() && location.endsWith(".jar")) {
                return file;
            }
            System.err.println("[PotionEnchantAgent] 主 jar 路径无效: " + location);
        } catch (Throwable t) {
            System.err.println("[PotionEnchantAgent] 解析主 jar 路径失败: " + t);
        }
        return null;
    }
}