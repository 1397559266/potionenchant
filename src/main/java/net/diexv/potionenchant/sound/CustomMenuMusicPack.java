package net.diexv.potionenchant.sound;

import net.diexv.potionenchant.client.MenuResourceScanner;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

/**
 * 自定义资源包，将 config/potionenchant/menu/music/ 中的音乐文件映射为
 * 虚拟资源路径 assets/potionenchant/sounds/menu_music_custom.ogg，
 * 使 Minecraft 声音引擎能够播放配置目录中的自定义 OGG 音乐。
 */
public class CustomMenuMusicPack implements PackResources {

    /** 虚拟声音事件名（必须在 sounds.json 和 ModSounds 中有对应条目） */
    public static final String VIRTUAL_SOUND_NAME = "menu_music_custom";
    public static final String PACK_ID = "potionenchant_menu_music";

    /** 当前自定义音乐的实际文件路径（为 null 表示无自定义音乐） */
    private static Path currentMusicPath = null;

    /**
     * 根据当前配置刷新自定义音乐文件路径
     * 仅在配置的是非内置文件名时设置路径
     */
    public static void refreshMusicPath() {
        currentMusicPath = null;
        try {
            String configName = PotionEnchantConfig.CLIENT.menuMusicFile.get();
            if (configName == null || configName.isEmpty() || "none".equalsIgnoreCase(configName)) {
                return;
            }
            // 检查是否是内置音乐名（内置的走 ForgeRegistries，不需要这个包）
            for (String builtin : MenuResourceScanner.BUILTIN_MUSIC) {
                if (builtin.equals(configName)) return;
            }
            // 查找外部自定义文件
            Path musicFile = MenuResourceScanner.getMusicPath(configName);
            if (musicFile != null && Files.exists(musicFile)) {
                currentMusicPath = musicFile;
            }
        } catch (Exception e) {
            // 配置尚未初始化等情况，静默忽略
        }
    }

    /** 判断当前是否有有效的自定义音乐文件 */
    public static boolean hasCustomMusic() {
        return currentMusicPath != null && Files.exists(currentMusicPath);
    }

    // ===== PackResources 实现 =====

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation loc) {
        if (type == PackType.CLIENT_RESOURCES
                && "potionenchant".equals(loc.getNamespace())
                && ("sounds/" + VIRTUAL_SOUND_NAME + ".ogg").equals(loc.getPath())
                && currentMusicPath != null && Files.exists(currentMusicPath)) {
            return IoSupplier.create(currentMusicPath);
        }
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        // 无需枚举
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES
                ? Collections.singleton("potionenchant")
                : Collections.emptySet();
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        if ("pack".equals(serializer.getMetadataSectionName())) {
            return (T) new PackMetadataSection(
                    Component.literal("PotionEnchant Custom Menu Music"),
                    15  // 1.20.1 pack format
            );
        }
        return null;
    }

    @Override
    public String packId() {
        return PACK_ID;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public void close() {
    }
}
