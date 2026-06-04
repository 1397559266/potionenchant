package net.diexv.potionenchant.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class RangeExtensionEffect extends MobEffect {

    // 属性修饰符的UUID（确保唯一性）
    private static final UUID REACH_DISTANCE_MODIFIER_ID = UUID.fromString("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d");
    private static final UUID ATTACK_RANGE_MODIFIER_ID = UUID.fromString("2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e");

    public RangeExtensionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x800080); // 紫色
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance() != null &&
                event.getEffectInstance().getEffect() instanceof RangeExtensionEffect &&
                event.getEntity() instanceof Player player) {
            applyRangeModifiers(player, event.getEffectInstance().getAmplifier());
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null &&
                event.getEffectInstance().getEffect() instanceof RangeExtensionEffect &&
                event.getEntity() instanceof Player player) {
            removeRangeModifiers(player);
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null &&
                event.getEffectInstance().getEffect() instanceof RangeExtensionEffect &&
                event.getEntity() instanceof Player player) {
            removeRangeModifiers(player);
        }
    }

    private static void applyRangeModifiers(Player player, int amplifier) {
        // 计算增加的格数：每级增加0.5格
        double rangeIncrease = (amplifier + 1) * 0.5;

        // 应用交互距离修饰符
        AttributeInstance reachDistance = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reachDistance != null) {
            // 先移除可能存在的旧修饰符
            reachDistance.removeModifier(REACH_DISTANCE_MODIFIER_ID);
            // 添加新的修饰符
            reachDistance.addTransientModifier(new AttributeModifier(
                    REACH_DISTANCE_MODIFIER_ID,
                    "Range extension reach distance",
                    rangeIncrease,
                    AttributeModifier.Operation.ADDITION
            ));
        }

        // 应用攻击距离修饰符
        AttributeInstance attackRange = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (attackRange != null) {
            // 先移除可能存在的旧修饰符
            attackRange.removeModifier(ATTACK_RANGE_MODIFIER_ID);
            // 添加新的修饰符
            attackRange.addTransientModifier(new AttributeModifier(
                    ATTACK_RANGE_MODIFIER_ID,
                    "Range extension attack range",
                    rangeIncrease,
                    AttributeModifier.Operation.ADDITION
            ));
        }
    }

    private static void removeRangeModifiers(Player player) {
        // 移除交互距离修饰符
        AttributeInstance reachDistance = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reachDistance != null) {
            reachDistance.removeModifier(REACH_DISTANCE_MODIFIER_ID);
        }

        // 移除攻击距离修饰符
        AttributeInstance attackRange = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (attackRange != null) {
            attackRange.removeModifier(ATTACK_RANGE_MODIFIER_ID);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 这个效果不需要每刻都执行特殊逻辑，属性修改已经在事件中处理
        return false;
    }
}