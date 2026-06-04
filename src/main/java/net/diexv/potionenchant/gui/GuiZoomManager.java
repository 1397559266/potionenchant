package net.diexv.potionenchant.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理 GUI 缩放设置的持久化存储（保存到 JSON 文件）。
 * 关闭 GUI 时自动保存缩放级别；打开 GUI 时自动恢复。
 */
public class GuiZoomManager {

    private static final String FILE_NAME = "potionenchant-gui-zoom.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 默认缩放值 */
    private static final float DEFAULT_LEVEL = 1.0f;

    /** 已加载的内存缓存 */
    private static Map<String, ZoomData> cache = null;

    // ---- 内部数据模型 ----
    public static class ZoomData {
        public float level = DEFAULT_LEVEL;
        public float headerLevel = DEFAULT_LEVEL;
    }

    // ======================== 公开 API ========================

    /** 获取指定 GUI 的缩放数据（首次调用时从磁盘载入） */
    public static ZoomData get(String screenId) {
        ensureLoaded();
        return cache.computeIfAbsent(screenId, k -> new ZoomData());
    }

    /** 保存指定 GUI 的缩放数据到磁盘 */
    public static void save(String screenId, float level, float headerLevel) {
        ensureLoaded();
        ZoomData data = cache.computeIfAbsent(screenId, k -> new ZoomData());
        data.level = level;
        data.headerLevel = headerLevel;
        writeToDisk();
    }

    /** 立即将内存缓存写入磁盘 */
    public static void flush() {
        if (cache != null) writeToDisk();
    }

    // ======================== 内部实现 ========================

    private static File getFile() {
        Minecraft mc = Minecraft.getInstance();
        File configDir = new File(mc.gameDirectory, "config");
        if (!configDir.exists()) configDir.mkdirs();
        return new File(configDir, FILE_NAME);
    }

    private static void ensureLoaded() {
        if (cache != null) return;
        cache = new HashMap<>();
        File file = getFile();
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, ZoomData>>() {}.getType();
            Map<String, ZoomData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) cache = loaded;
        } catch (Exception e) {
            // 文件损坏时忽略，使用默认值
            cache = new HashMap<>();
        }
    }

    private static void writeToDisk() {
        File file = getFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(cache, writer);
            writer.flush();
        } catch (Exception e) {
            // 写文件失败时静默处理
        }
    }
}
