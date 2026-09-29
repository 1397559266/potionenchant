package net.diexv.potionenchant.sound;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, PotionEnchantMod.MODID);

    public static final RegistryObject<SoundEvent> MENU_MUSIC = register("menu_music");
    public static final RegistryObject<SoundEvent> MENU_MUSIC_2 = register("menu_music_2");
    /** 虚拟声音事件，用于播放 config/potionenchant/menu/music/ 中的自定义音乐 */
    public static final RegistryObject<SoundEvent> MENU_MUSIC_CUSTOM = register(CustomMenuMusicPack.VIRTUAL_SOUND_NAME);
    public static final RegistryObject<SoundEvent> SPRINT = register("sprint");
    /** 左键挥砍音效（X剑 / DiexvSword） */
    public static final RegistryObject<SoundEvent> SWORD_SWING = register("sword_swing");

    // ===== DiexvSword 激光音效（蓄力/发射/飞行） =====
    public static final RegistryObject<SoundEvent> DIEXV_CREEPER_RAY = registerFixed("diexvcreeper/ray", 128);
    public static final RegistryObject<SoundEvent> DIEXV_CREEPER_RAY_OS = registerFixed("diexvcreeper/ray_os", 128);
    public static final RegistryObject<SoundEvent> DIEXV_CREEPER_RAY_SKYLANCE = registerFixed("diexvcreeper/ray_skylance", 128);
public static final RegistryObject<SoundEvent> DIEXV_CREEPER_WARNING = registerFixed("diexvcreeper/waring", 128);

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(PotionEnchantMod.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    private static RegistryObject<SoundEvent> registerFixed(String name, float range) {
        ResourceLocation id = new ResourceLocation(PotionEnchantMod.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createFixedRangeEvent(id, range));
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
