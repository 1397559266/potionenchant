package net.diexv.potionenchant.item;

import net.diexv.potionenchant.client.DiexvClientItemExtensions;
import net.diexv.potionenchant.util.XSwordTargetTracker;
import net.diexv.potionenchant.client.font.DiexvFont;
import net.diexv.potionenchant.client.font.DiexvFont3;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class XSwordItem extends SwordItem {

    private static final double DASH_STRENGTH = 2.5;

    private static final Map<UUID, Boolean> SUPERMODE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_TOGGLE_TIME = new ConcurrentHashMap<>();
    private static final long TOGGLE_DEBOUNCE_MS = 100;

    private static final Map<UUID, Float> BLOCKING_HEALTH = new ConcurrentHashMap<>();

    private static final Map<UUID, Boolean> DASH_AIRBORNE = new ConcurrentHashMap<>();

    public static void clearSupermodeState(UUID playerUuid) {
        SUPERMODE.remove(playerUuid);
        LAST_TOGGLE_TIME.remove(playerUuid);
        BLOCKING_HEALTH.remove(playerUuid);
        DASH_AIRBORNE.remove(playerUuid);
    }

    public static boolean isSupermode(UUID playerUuid) {
        return SUPERMODE.getOrDefault(playerUuid, false);
    }

    /**
     * 玩家是否手持X剑（主手或副手）。
     * 超级模式的伤害/无敌效果只应在真正手持X剑时生效，
     * 防止放下/丢弃X剑后仍保留秒杀或无敌效果。
     */
    public static boolean isHoldingXSword(Player player) {
        return player.getMainHandItem().getItem() instanceof XSwordItem
            || player.getOffhandItem().getItem() instanceof XSwordItem;
    }

    @Nullable
    public static Float getBlockingHealth(UUID uuid) {
        return BLOCKING_HEALTH.get(uuid);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        SUPERMODE.remove(uuid);
        LAST_TOGGLE_TIME.remove(uuid);
        BLOCKING_HEALTH.remove(uuid);
        DASH_AIRBORNE.remove(uuid);
    }


    public XSwordItem(Tier tier, int damage, float attackSpeed, Properties properties) {
        super(tier, damage, attackSpeed, properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isLocalPlayerInSupermode() {
        Player player = Minecraft.getInstance().player;
        return player != null && isSupermode(player.getUUID());
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        if (FMLEnvironment.dist == Dist.CLIENT && isLocalPlayerInSupermode()) {
            return Component.literal("搂4搂");
        }
        return Component.translatable("item.potionenchant.x_sword");
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        boolean isSuper = FMLEnvironment.dist == Dist.CLIENT && isLocalPlayerInSupermode();
        if (isSuper) {
            // Replace attack damage/speed values with ???
            String dmgKey = "attribute.name.generic.attack_damage";
            String spdKey = "attribute.name.generic.attack_speed";
            String dmgName = Component.translatable(dmgKey).getString();
            String spdName = Component.translatable(spdKey).getString();
            for (int i = 0; i < tooltip.size(); i++) {
                String text = tooltip.get(i).getString();
                if (text.contains(dmgName) || text.contains(spdName)) {
                    tooltip.set(i, Component.literal("???"));
                }
            }
            tooltip.add(Component.literal("??????????").withStyle(ChatFormatting.OBFUSCATED));
        } else {
            tooltip.add(Component.translatable("item.potionenchant.x_sword.mode",
                    Component.translatable("item.potionenchant.x_sword.normal")).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.potionenchant.x_sword.mode_switch_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    @SuppressWarnings("removal")
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return;
        UUID uuid = player.getUUID();
        boolean isUsing = player.isUsingItem() && player.getUseItem().getItem() instanceof XSwordItem;

        if (isUsing && !isSupermode(uuid)) {
            if (!BLOCKING_HEALTH.containsKey(uuid)) {
                BLOCKING_HEALTH.put(uuid, player.getHealth());
            }
            return;
        }

        if (!isUsing && !isSupermode(uuid)) {
            BLOCKING_HEALTH.remove(uuid);
        }

        // Supermode: dash AOE every tick while airborne
        if (isSupermode(uuid) && DASH_AIRBORNE.getOrDefault(uuid, false)) {
            if (player.onGround()) {
                DASH_AIRBORNE.remove(uuid);
            } else {
                Vec3 center = player.position();
                AABB area = new AABB(
                    center.x - 5, center.y - 5, center.z - 5,
                    center.x + 5, center.y + 5, center.z + 5);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());
                for (LivingEntity target : targets) {
                    target.hurt(player.damageSources().playerAttack(player), 0);
                }
            }
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return getClientUseAnim();
        }
        return UseAnim.BLOCK;
    }

    @OnlyIn(Dist.CLIENT)
    private UseAnim getClientUseAnim() {
        Player player = Minecraft.getInstance().player;
        if (player != null && isSupermode(player.getUUID())) {
            return UseAnim.BOW;
        }
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack itemstack) {
        return 100000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
        InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
        entity.startUsingItem(hand);
        return ar;
    }

    @Override
    @SuppressWarnings("removal")
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof Player player)) {
            super.releaseUsing(stack, level, entity, timeCharged);
            return;
        }

        UUID uuid = player.getUUID();

        if (!isSupermode(uuid)) {
            BLOCKING_HEALTH.remove(uuid);
            super.releaseUsing(stack, level, entity, timeCharged);
            return;
        }

        // 释放时执行冲刺（仅在服务端执行）
        BLOCKING_HEALTH.remove(uuid);

        if (!level.isClientSide) {
            Vec3 look = player.getLookAngle();
            Vec3 dashVelocity = look.scale(DASH_STRENGTH);
            player.setDeltaMovement(player.getDeltaMovement().add(dashVelocity));
            player.hurtMarked = true;
            player.setOnGround(false);

            // 不设置 setSprinting(true)：冲刺速度已由 setDeltaMovement 提供，
            // 强行开启疾跑会让客户端 FOV 反复伸缩；与《永恒枪械工坊：零》(TACZ) 等
            // 枪械模组同时加载时，其相机/开镜系统会把这个 FOV 抖动放大成视角抽搐。
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new ResourceLocation("potionenchant", "sprint")),
                    SoundSource.PLAYERS, 1.0F, 1.0F);

            player.fallDistance = 0;
            player.resetFallDistance();
        }

        DASH_AIRBORNE.put(uuid, true);

        super.releaseUsing(stack, level, entity, timeCharged);
    }

    /** 左键挥动的 360° 范围伤害：攻击距离（水平半径） */
    private static final double SWEEP_RADIUS = 3.5D;
    /** 垂直方向的容忍高度（相对玩家包围盒上下扩展） */
    private static final double SWEEP_VERTICAL = 2.0D;

    /**
     * 360° 范围攻击：攻击距离内的所有实体都吃一次"XSword 左键攻击"的效果 ——
     * 与原版玩家攻击同一套结算：同源伤害（playerAttack）、同样的属性伤害与蓄力缩放、
     * 同样的附魔加成（锋利/亡灵杀手/节肢杀手）与附魔后效（火焰附加、击退等）。
     *
     * <p>不特意排除"当前直接命中的那只"：原版受击无敌帧（invulnerableTime）本身就会免疫
     * 二次伤害，所以直接命中的目标不会被同一刀打两遍。
     */
    private static void sweepAttack(net.minecraft.server.level.ServerPlayer player, ItemStack stack) {
        Level level = player.level();
        AABB area = player.getBoundingBox().inflate(SWEEP_RADIUS, SWEEP_VERTICAL, SWEEP_RADIUS);
        double maxSqr = SWEEP_RADIUS * SWEEP_RADIUS;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e ->
                e != player
                        && e.isAlive()
                        && !e.isSpectator()
                        && !player.isAlliedTo(e)
                        && !player.isPassengerOfSameVehicle(e)
                        && (!(e instanceof net.minecraft.world.entity.decoration.ArmorStand stand) || !stand.isMarker())
                        && player.distanceToSqr(e) <= maxSqr);
        if (targets.isEmpty()) {
            return;
        }

        net.minecraft.world.damagesource.DamageSource source = player.damageSources().playerAttack(player);
        float attackDamage = (float) player.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float charge = player.getAttackStrengthScale(0.5F);
        float scale = 0.2F + charge * charge * 0.8F;      // 与原版一致的蓄力缩放（连点伤害低）

        boolean hitAny = false;
        for (LivingEntity target : targets) {
            float damage = attackDamage * scale + net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getDamageBonus(stack, target.getMobType());
            if (target.hurt(source, damage)) {
                hitAny = true;
                net.minecraft.world.item.enchantment.EnchantmentHelper.doPostHurtEffects(target, player);
                net.minecraft.world.item.enchantment.EnchantmentHelper.doPostDamageEffects(player, target);
            }
        }

        if (hitAny) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                        player.getX() - net.minecraft.util.Mth.sin(yaw),
                        player.getY() + player.getBbHeight() * 0.5D,
                        player.getZ() + net.minecraft.util.Mth.cos(yaw),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && player.isShiftKeyDown()) {
            UUID uuid = player.getUUID();
            long now = System.currentTimeMillis();
            Long last = LAST_TOGGLE_TIME.get(uuid);

            if (last != null && now - last < TOGGLE_DEBOUNCE_MS) {
                return true;
            }
            LAST_TOGGLE_TIME.put(uuid, now);

            SUPERMODE.put(uuid, !isSupermode(uuid));
            BLOCKING_HEALTH.remove(uuid);

            if (player.level().isClientSide) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.0F);
            }

            return true;
        }

        // 左键挥动：360° 范围伤害（ServerPlayer 只存在于服务端，天然只结算一次）
        if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            sweepAttack(serverPlayer, stack);
        }

        // 返回 false：不取消原版挥动（挥砍动画与挥砍音效照常）
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new DiexvClientItemExtensions() {
            @Override
            public @NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext context) {
                Player player = Minecraft.getInstance().player;
                if (player != null && isSupermode(player.getUUID())) {
                    return DiexvFont3.getFont();
                }
                return DiexvFont.getFont();
            }

            /** 左键攻击动画 */
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                   net.minecraft.client.player.LocalPlayer player,
                                                   net.minecraft.world.entity.HumanoidArm arm,
                                                   ItemStack itemInHand, float partialTick,
                                                   float equipProcess, float swingProcess) {
                if (net.diexv.potionenchant.client.renderer.CutterAttackAnimation.apply(poseStack, swingProcess,
                        arm == net.minecraft.world.entity.HumanoidArm.LEFT)) {
                    return true;
                }
                return super.applyForgeHandTransform(poseStack, player, arm, itemInHand, partialTick,
                        equipProcess, swingProcess);
            }
        });
    }
}
