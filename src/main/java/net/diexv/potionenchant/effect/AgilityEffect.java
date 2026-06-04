package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class AgilityEffect extends MobEffect {

    private static final UUID SPRINTING_SPEED_MODIFIER_ID = UUID.fromString("3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f");
    private static final UUID ATTACK_SPEED_MODIFIER_ID = UUID.fromString("4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a");

    public AgilityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFA500);
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance() != null &&
                event.getEffectInstance().getEffect() instanceof AgilityEffect &&
                event.getEntity() instanceof Player player) {
            applyAttackSpeedModifier(player, event.getEffectInstance().getAmplifier());
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null &&
                event.getEffectInstance().getEffect() instanceof AgilityEffect &&
                event.getEntity() instanceof Player player) {
            removeAllModifiers(player);
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null &&
                event.getEffectInstance().getEffect() instanceof AgilityEffect &&
                event.getEntity() instanceof Player player) {
            removeAllModifiers(player);
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player &&
                player.hasEffect(EffectRegistry.AGILITY.get())) {
            updateSprintingSpeed(player);
        }
    }

    private static void applyAttackSpeedModifier(Player player, int amplifier) {
        // 计算攻击速度加成：每级增加50%
        double attackSpeedMultiplier = 0.50 * (amplifier + 1);

        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_ID);
            attackSpeed.addTransientModifier(new AttributeModifier(
                    ATTACK_SPEED_MODIFIER_ID,
                    "Speed boost attack speed",
                    attackSpeedMultiplier,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    private static void updateSprintingSpeed(Player player) {
        int amplifier = player.getEffect(EffectRegistry.AGILITY.get()).getAmplifier();
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movementSpeed != null) {
            movementSpeed.removeModifier(SPRINTING_SPEED_MODIFIER_ID);

            if (player.isSprinting()) {
                // 计算移动速度加成：原版速度药水的1.5倍
                double movementSpeedMultiplier = 0.30 * (amplifier + 1); // +30% 每级

                movementSpeed.addTransientModifier(new AttributeModifier(
                        SPRINTING_SPEED_MODIFIER_ID,
                        "Speed boost sprinting speed",
                        movementSpeedMultiplier,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }
        }
    }

    private static void removeAllModifiers(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(SPRINTING_SPEED_MODIFIER_ID);
        }

        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_ID);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // 我们使用事件监听器，所以不需要每刻都调用
    }
}