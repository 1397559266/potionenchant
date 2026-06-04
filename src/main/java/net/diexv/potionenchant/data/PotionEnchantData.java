package net.diexv.potionenchant.data;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.nbt.CompoundTag;
import net.diexv.potionenchant.util.helper.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class PotionEnchantData {
    private final MobEffect effect;
    private final int amplifier;
    private final boolean isArmorEnchant;
    private int color = 0xFFFFFF; // 默认白色

    public PotionEnchantData(MobEffect effect, int amplifier, boolean isArmorEnchant) {
        this.effect = effect;
        this.amplifier = amplifier;
        this.isArmorEnchant = isArmorEnchant;
        // 尝试获取效果的颜色
        try {
            this.color = effect.getColor();
        } catch (Exception e) {
            this.color = 0xFFFFFF; // 如果获取失败使用白色
        }
    }

    public PotionEnchantData(MobEffect effect, int amplifier, boolean isArmorEnchant, int color) {
        this.effect = effect;
        this.amplifier = amplifier;
        this.isArmorEnchant = isArmorEnchant;
        this.color = color;
    }

    public MobEffect getEffect() {
        return effect;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public boolean isArmorEnchant() {
        return isArmorEnchant;
    }

    public int getColor() {
        // 始终返回效果自身的颜色，忽略存储的color（可能来自药水瓶混合色）
        try {
            return effect.getColor();
        } catch (Exception e) {
            return color;
        }
    }

    public void setColor(int color) {
        this.color = color;
    }

    // 为工具应用效果到目标实体
    public void applyToolEffect(net.minecraft.world.entity.LivingEntity target, int level) {
        int duration = 200; // 10秒
        target.addEffect(new MobEffectInstance(effect, duration, level - 1));
    }


    // 为盔甲应用效果到穿戴者
    public void applyArmorEffect(net.minecraft.world.entity.LivingEntity wearer, int level) {
        if (isArmorEnchant) {
            int duration = 200; // 10秒
            wearer.addEffect(new MobEffectInstance(effect, duration, level - 1));
        }
    }

    // 保存数据到NBT
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Effect", ForgeRegistries.MOB_EFFECTS.getKey(effect).toString());
        tag.putInt("Amplifier", amplifier);
        tag.putBoolean("IsArmor", isArmorEnchant);
        tag.putInt("Color", color);
        return tag;
    }

    // 从NBT创建数据实例
    public static PotionEnchantData loadFromNBT(CompoundTag tag) {
        ResourceLocation effectId = ResourceLocationHelper.parse(tag.getString("Effect"));
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        int amplifier = tag.getInt("Amplifier");
        boolean isArmor = tag.getBoolean("IsArmor");
        int color = tag.contains("Color") ? tag.getInt("Color") : 0xFFFFFF;

        if (effect != null) {
            return new PotionEnchantData(effect, amplifier, isArmor, color);
        }
        return null;
    }

    // 获取显示名称
    public String getDisplayName() {
        String effectName = effect.getDisplayName().getString();
        String level = getRomanNumeral(amplifier + 1);
        return effectName + " " + level;
    }

    private String getRomanNumeral(int number) {
        if (number < 1 || number > 10) return String.valueOf(number);

        String[] numerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return numerals[number - 1];
    }
}
