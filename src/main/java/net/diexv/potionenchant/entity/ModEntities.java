package net.diexv.potionenchant.entity;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = PotionEnchantMod.MODID)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PotionEnchantMod.MODID);
    
    // X护甲远程攻击Bomb（兼具环绕待命和攻击功能）
    public static final RegistryObject<EntityType<BombEntity>> BOMB = 
        ENTITIES.register("bomb", () -> 
            EntityType.Builder.<BombEntity>of(BombEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build("bomb")
        );
    
    // 彩虹闪电实体
    public static final RegistryObject<EntityType<RainbowLightningBolt>> RAINBOW_LIGHTNING = 
        ENTITIES.register("rainbow_lightning", () -> 
            EntityType.Builder.<RainbowLightningBolt>of(RainbowLightningBolt::new, MobCategory.MISC)
                .sized(0.0F, 0.0F)
                .clientTrackingRange(16)
                .build("rainbow_lightning")
        );
    
    // X护甲范围伤害实体（右键生成）
    public static final RegistryObject<EntityType<XBlockEntity>> XBLOCK = 
        ENTITIES.register("xblock", () -> 
            EntityType.Builder.<XBlockEntity>of(XBlockEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.0f) // 基础尺寸1x1，缩放由渲染器处理
                .clientTrackingRange(64)
                .updateInterval(1)
                .build("xblock")
        );
    
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(XBLOCK.get(), XBlockEntity.createAttributes().build());
    }
}
