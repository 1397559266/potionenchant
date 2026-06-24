package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.config.PotionEnchantConfig;
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
 * 这样音乐在主菜单、选项界面、世界选择界面都会持续播放
 * 直到进入游戏存档（玩家实体出现）后自动切回原版游戏内音乐
 */
@Mixin(Minecraft.class)
public class TitleScreenMusicMixin {

    private static Boolean hasCustomMusic = null;

    @Inject(method = "getSituationalMusic", at = @At("HEAD"), cancellable = true)
    private void onGetSituationalMusic(CallbackInfoReturnable<Music> cir) {
        // 仅在配置启用时替换
        if (!PotionEnchantConfig.COMMON.enableCustomMainMenu.get()) return;
        if (!checkMusicExists()) return;

        // 当没有玩家时（仍在菜单界面），播放自定义音乐
        Minecraft mc = (Minecraft)(Object)this;
        if (mc.player != null) return;  // 进入存档后不干预，用原版游戏内音乐

        List<? extends String> musicList = PotionEnchantConfig.COMMON.customMainMenuMusic.get();
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

        // 循环播放，无延迟
        Holder<SoundEvent> holder = Holder.direct(soundEvent);
        Music customMusic = new Music(holder, 0, 0, true);
        cir.setReturnValue(customMusic);
    }

    /**
     * 检测自定义音效注册 + OGG 文件是否都存在
     */
    private static boolean checkMusicExists() {
        if (hasCustomMusic == null) {
            try {
                List<? extends String> musicList = PotionEnchantConfig.COMMON.customMainMenuMusic.get();
                if (musicList == null || musicList.isEmpty()) {
                    hasCustomMusic = false;
                    return false;
                }

                String musicId = musicList.get(0);
                ResourceLocation soundRL = new ResourceLocation(musicId);

                // 检查注册表
                boolean registered = ForgeRegistries.SOUND_EVENTS.getValue(soundRL) != null;

                // 检查 OGG 文件
                ResourceLocation fileRL = new ResourceLocation(soundRL.getNamespace(), "sounds/" + soundRL.getPath() + ".ogg");
                ResourceManager rm = Minecraft.getInstance().getResourceManager();
                boolean fileExists = rm != null && rm.getResource(fileRL).isPresent();

                hasCustomMusic = registered && fileExists;
            } catch (Exception e) {
                hasCustomMusic = false;
            }
        }
        return hasCustomMusic;
    }
}