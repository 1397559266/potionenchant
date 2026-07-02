package net.diexv.potionenchant.client;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 扫描 config/potionenchant/menu/ 子文件夹中的资源文件
 * 目录结构：
 *   config/
 *     potionenchant/
 *       menu/
 *         icon/   -- 图标/Logo图片 (.png)
 *         menu/   -- 主菜单背景图片 (.png)
 *         music/  -- 主菜单音乐 (.ogg)
 */
public class MenuResourceScanner {

    // ===== 模组内置资源（来自 assets/potionenchant/textures/gui/ 和 sounds/） =====
    public static final String[] BUILTIN_BACKGROUNDS = {
        "main_menu_bg", "main_menu_bg_2"
    };
    public static final String[] BUILTIN_ICONS = {
        "main_menu_logo", "main_menu_logo_2", "main_menu_logo_3"
    };
    public static final String[] BUILTIN_MUSIC = {
        "menu_music", "menu_music_2"
    };

    private static Path configDir = null;
    private static Path baseDir = null;

    /**
     * 获取 config/potionenchant/menu/ 根目录
     */
    public static Path getBaseDir() {
        if (baseDir == null) {
            configDir = FMLPaths.CONFIGDIR.get();
            baseDir = configDir.resolve("potionenchant/menu");
            try {
                Files.createDirectories(baseDir);
            } catch (IOException e) {
                PotionEnchantMod.LOGGER.warn("[MenuResourceScanner] failed to create base dir", e);
            }
        }
        return baseDir;
    }

    /**
     * 获取指定子文件夹路径
     */
    public static Path getSubDir(String sub) {
        Path dir = getBaseDir().resolve(sub);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            PotionEnchantMod.LOGGER.warn("[MenuResourceScanner] failed to create sub dir: " + sub, e);
        }
        return dir;
    }

    /**
     * 列出子文件夹中所有匹配后缀的文件名（不含后缀）
     */
    public static List<String> listFiles(String sub, String extension) {
        List<String> result = new ArrayList<>();
        Path dir = getSubDir(sub);
        if (!Files.isDirectory(dir)) return result;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + extension)) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                String nameWithoutExt = fileName.substring(0, fileName.length() - extension.length());
                if (!nameWithoutExt.isEmpty()) {
                    result.add(nameWithoutExt);
                }
            }
        } catch (IOException e) {
            PotionEnchantMod.LOGGER.warn("[MenuResourceScanner] failed to list " + sub + " files", e);
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * 获取菜单背景图片列表（.png）
     */
    public static List<String> listMenuBackgrounds() {
        return listFiles("menu", ".png");
    }

    /**
     * 获取图标图片列表（.png）
     */
    public static List<String> listIcons() {
        return listFiles("icon", ".png");
    }

    /**
     * 获取音乐文件列表（.ogg）
     */
    public static List<String> listMusicFiles() {
        return listFiles("music", ".ogg");
    }

    /**
     * 获取指定背景图片的完整路径（仅对自定义文件，内置返回null）
     */
    public static Path getBackgroundPath(String name) {
        if (name == null || name.isEmpty()) return null;
        for (String b : BUILTIN_BACKGROUNDS) {
            if (b.equals(name)) return null;
        }
        Path p = getSubDir("menu").resolve(name + ".png");
        return Files.exists(p) ? p : null;
    }

    /**
     * 获取指定图标图片的完整路径（仅对自定义文件，内置返回null）
     */
    public static Path getIconPath(String name) {
        if (name == null || name.isEmpty()) return null;
        for (String b : BUILTIN_ICONS) {
            if (b.equals(name)) return null;
        }
        Path p = getSubDir("icon").resolve(name + ".png");
        return Files.exists(p) ? p : null;
    }

    /**
     * 获取指定音乐文件的完整路径（仅对自定义文件，内置返回null）
     */
    public static Path getMusicPath(String name) {
        if (name == null || name.isEmpty()) return null;
        for (String b : BUILTIN_MUSIC) {
            if (b.equals(name)) return null;
        }
        Path p = getSubDir("music").resolve(name + ".ogg");
        return Files.exists(p) ? p : null;
    }

    /**
     * 获取背景选项列表（内置优先 + 自定义）
     */
    public static String[] getBackgroundOptions() {
        List<String> list = new ArrayList<>();
        for (String s : BUILTIN_BACKGROUNDS) {
            list.add(s);
        }
        for (String s : listMenuBackgrounds()) {
            if (!list.contains(s)) list.add(s);
        }
        return list.toArray(new String[0]);
    }

    /**
     * 获取图标选项列表（内置优先 + 自定义）
     */
    public static String[] getIconOptions() {
        List<String> list = new ArrayList<>();
        for (String s : BUILTIN_ICONS) {
            list.add(s);
        }
        for (String s : listIcons()) {
            if (!list.contains(s)) list.add(s);
        }
        return list.toArray(new String[0]);
    }

    /**
     * 获取音乐选项列表（内置优先 + 自定义）
     */
    public static String[] getMusicOptions() {
        List<String> list = new ArrayList<>();
        for (String s : BUILTIN_MUSIC) {
            list.add(s);
        }
        for (String s : listMusicFiles()) {
            if (!list.contains(s)) list.add(s);
        }
        return list.toArray(new String[0]);
    }

    /**
     * 验证文件名是否有效（内置或存在于对应文件夹）
     */
    public static boolean isValidFile(String sub, String extension, String name) {
        if (name == null || name.isEmpty()) return false;
        if ("menu".equals(sub)) {
            for (String b : BUILTIN_BACKGROUNDS) { if (b.equals(name)) return true; }
        } else if ("icon".equals(sub)) {
            for (String b : BUILTIN_ICONS) { if (b.equals(name)) return true; }
        } else if ("music".equals(sub)) {
            for (String b : BUILTIN_MUSIC) { if (b.equals(name)) return true; }
        }
        return Files.exists(getSubDir(sub).resolve(name + extension));
    }
}
