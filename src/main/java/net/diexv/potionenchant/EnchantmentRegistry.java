package net.diexv.potionenchant;

import net.diexv.potionenchant.enchantments.AdvancedMendingEnchantment;
import net.diexv.potionenchant.enchantments.AdvancedPowerEnchantment;
import net.diexv.potionenchant.enchantments.AdvancedProtectionEnchantment;
import net.diexv.potionenchant.enchantments.AdvancedSharpnessEnchantment;
import net.diexv.potionenchant.enchantments.AshExtinctionEnchantment;
import net.diexv.potionenchant.enchantments.AutoSmeltEnchantment;
import net.diexv.potionenchant.enchantments.BarrageEnchantment;
import net.diexv.potionenchant.enchantments.BlazeAspectEnchantment;
import net.diexv.potionenchant.enchantments.ComboEnchantment;
import net.diexv.potionenchant.enchantments.CriticalStrikeEnchantment;
import net.diexv.potionenchant.enchantments.DamageStorageEnchantment;
import net.diexv.potionenchant.enchantments.ElementalAffinityEnchantment;
import net.diexv.potionenchant.enchantments.LifeLinkEnchantment;
import net.diexv.potionenchant.enchantments.LifestealEnchantment;
import net.diexv.potionenchant.enchantments.ManaFocusEnchantment;
import net.diexv.potionenchant.enchantments.PotionBaneEnchantment;
import net.diexv.potionenchant.enchantments.ReforgeEnchantment;
import net.diexv.potionenchant.enchantments.TrackingArrowEnchantment;
import net.diexv.potionenchant.enchantments.WitherAspectEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EnchantmentRegistry {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, PotionEnchantMod.MODID);

    public static final RegistryObject<Enchantment> ELEMENTAL_AFFINITY =
            ENCHANTMENTS.register("elemental_affinity", ElementalAffinityEnchantment::new);
    public static final RegistryObject<Enchantment> REFORGE =
            ENCHANTMENTS.register("reforge", ReforgeEnchantment::new);
    public static final RegistryObject<Enchantment> LIFESTEAL =
            ENCHANTMENTS.register("lifesteal", LifestealEnchantment::new);
    public static final RegistryObject<Enchantment> ADVANCED_MENDING =
            ENCHANTMENTS.register("advanced_mending", AdvancedMendingEnchantment::new);
    public static final RegistryObject<Enchantment> ASH_EXTINCTION =
            ENCHANTMENTS.register("ash_extinction", AshExtinctionEnchantment::new);
    public static final RegistryObject<Enchantment> COMBO =
            ENCHANTMENTS.register("combo", ComboEnchantment::new);
    public static final RegistryObject<Enchantment> TRACKING_ARROW =
            ENCHANTMENTS.register("tracking_arrow", TrackingArrowEnchantment::new);
    public static final RegistryObject<Enchantment> BARRAGE =
            ENCHANTMENTS.register("barrage", BarrageEnchantment::new);
    public static final RegistryObject<Enchantment> BLAZE_ASPECT =
            ENCHANTMENTS.register("blaze_aspect", BlazeAspectEnchantment::new);
    public static final RegistryObject<Enchantment> CRITICAL_STRIKE =
            ENCHANTMENTS.register("critical_strike", CriticalStrikeEnchantment::new);
    public static final RegistryObject<Enchantment> LIFE_LINK =
            ENCHANTMENTS.register("life_link", LifeLinkEnchantment::new);
    public static final RegistryObject<Enchantment> AUTO_SMELT =
            ENCHANTMENTS.register("auto_smelt", AutoSmeltEnchantment::new);
    public static final RegistryObject<Enchantment> WITHER_ASPECT =
            ENCHANTMENTS.register("wither_aspect", WitherAspectEnchantment::new);
    public static final RegistryObject<Enchantment> POTION_BANE =
            ENCHANTMENTS.register("potion_bane", PotionBaneEnchantment::new);
    public static final RegistryObject<Enchantment> ADVANCED_SHARPNESS =
            ENCHANTMENTS.register("advanced_sharpness", AdvancedSharpnessEnchantment::new);
    public static final RegistryObject<Enchantment> ADVANCED_PROTECTION =
            ENCHANTMENTS.register("advanced_protection", AdvancedProtectionEnchantment::new);
    public static final RegistryObject<Enchantment> MANA_FOCUS =
            ENCHANTMENTS.register("mana_focus", ManaFocusEnchantment::new);
    public static final RegistryObject<Enchantment> DAMAGE_STORAGE =
            ENCHANTMENTS.register("damage_storage", DamageStorageEnchantment::new);
    public static final RegistryObject<Enchantment> ADVANCED_POWER =
            ENCHANTMENTS.register("advanced_power", AdvancedPowerEnchantment::new);
}
