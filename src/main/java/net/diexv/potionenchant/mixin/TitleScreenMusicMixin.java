package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.MenuResourceScanner;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.sound.CustomMenuMusicPack;
import net.diexv.potionenchant.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 主菜单背景音乐替换
 * 注入 Minecraft.getSituationalMusic()，在没有玩家（仍在菜单界面）时播放自定义音乐
 * 支持：
 *   1. 内置音乐（menu_music, menu_music_2）→ 从 ForgeRegistries 查找
 *   2. 自定义外部音乐（config/potionenchant/menu/music/ 中的 .ogg）→ 通过虚拟资源包播放
 */
@Mixin(Minecraft.class)
public class TitleScreenMusicMixin {

    @Inject(method = "getSituationalMusic", at = @At("HEAD"), cancellable = true)
    private void onGetSituationalMusic(CallbackInfoReturnable<Music> cir) {
        if (!PotionEnchantConfig.CLIENT.enableCustomMainMenu.get()) return;
        if (!checkMusicExists()) return;

        Minecraft mc = (Minecraft)(Object)this;
        if (mc.player != null) return;

        String menuMusicFile = PotionEnchantConfig.CLIENT.menuMusicFile.get();
        java.util.List<String> musicList = getMusicList(menuMusicFile);
        if (musicList == null || musicList.isEmpty()) return;

        String musicId = musicList.get(0);
        SoundEvent soundEvent = null;

        try {
            ResourceLocation soundRL = new ResourceLocation(musicId);
            soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(soundRL);
        } catch (Exception e) {
            PotionEnchantMod.LOGGER.warn("[MenuMusic] Failed to parse sound id: {}", musicId);
            return;
        }

        if (soundEvent == null) {
            PotionEnchantMod.LOGGER.warn("[MenuMusic] Sound event not found: {}", musicId);
            return;
        }

        Holder<SoundEvent> holder = Holder.direct(soundEvent);
        Music customMusic = new Music(holder, 0, 0, true);
        cir.setReturnValue(customMusic);
    }

    /**
     * 检测声音事件 + OGG 文件是否都可用
     * 对内置音乐检查 ForgeRegistries + 资源 OGG
     * 对自定义外部音乐检查虚拟 SoundEvent 注册 + 自定义文件存在
     */
    private static boolean checkMusicExists() {
        try {
            String musicFile = PotionEnchantConfig.CLIENT.menuMusicFile.get();
            java.util.List<String> musicList = getMusicList(musicFile);
            if (musicList == null || musicList.isEmpty()) return false;

            String musicId = musicList.get(0);
            ResourceLocation soundRL = new ResourceLocation(musicId);

            // 检查注册表中有没有这个 SoundEvent
            boolean registered = ForgeRegistries.SOUND_EVENTS.getValue(soundRL) != null;
            if (!registered) return false;

            // 检查是否是内置音乐
            boolean isBuiltin = false;
            for (String b : MenuResourceScanner.BUILTIN_MUSIC) {
                if (b.equals(musicFile)) { isBuiltin = true; break; }
            }

            if (isBuiltin) {
                // 内置音乐：检查 assets 中的 OGG 文件
                ResourceLocation fileRL = new ResourceLocation(soundRL.getNamespace(),
                        "sounds/" + soundRL.getPath() + ".ogg");
                ResourceManager rm = Minecraft.getInstance().getResourceManager();
                return rm != null && rm.getResource(fileRL).isPresent();
            } else {
                // 自定义外部音乐：检查 CustomMenuMusicPack 是否有有效文件
                return CustomMenuMusicPack.hasCustomMusic();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取音乐 ID 列表
     * 优先规则：
     *   1. menuMusicFile 不为空/不为 none
     *      a. 如果是内置音乐名 → 返回 "potionenchant:<name>"
     *      b. 如果是自定义文件名 → 返回虚拟 SoundEvent ID
     *   2. customMainMenuMusic 列表兜底
     *   3. 最后 hardcode 为 "potionenchant:menu_music"
     */
    private static java.util.List<String> getMusicList(String menuMusicFile) {
        if (menuMusicFile != null && !menuMusicFile.isEmpty() && !"none".equalsIgnoreCase(menuMusicFile)) {
            // 检查是不是内置音乐
            boolean isBuiltin = false;
            for (String b : MenuResourceScanner.BUILTIN_MUSIC) {
                if (b.equals(menuMusicFile)) { isBuiltin = true; break; }
            }
            if (isBuiltin) {
                return java.util.Collections.singletonList("potionenchant:" + menuMusicFile);
            } else {
                // 自定义外部音乐 → 使用虚拟 SoundEvent
                return java.util.Collections.singletonList("potionenchant:" + CustomMenuMusicPack.VIRTUAL_SOUND_NAME);
            }
        }
        List<? extends String> fallback = PotionEnchantConfig.CLIENT.customMainMenuMusic.get();
        if (fallback != null && !fallback.isEmpty()) {
            return (java.util.List<String>) fallback;
        }
        return java.util.Collections.singletonList("potionenchant:menu_music");
    }
}
