package net.diexv.potionenchant.item;

import net.diexv.potionenchant.util.DiexvClientItemExtensions;
import net.diexv.potionenchant.util.XSwordTargetTracker;
import net.diexv.potionenchant.util.font.DiexvFont3;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        Player player = Minecraft.getInstance().player;
        if (player != null && isSupermode(player.getUUID())) {
            return Component.literal("搂4搂");
        }
        return Component.translatable("item.potionenchant.x_sword");
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean isSuper = isSupermode(player.getUUID());
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
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player p && isSupermode(p.getUUID())) {
            target.hurt(p.damageSources().playerAttack(p), Float.MAX_VALUE);
            return true;
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (isSupermode(player.getUUID()) && entity instanceof LivingEntity target) {
            target.hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
            return true;
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        if (!selected) return;

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

        // 释放时执行冲刺
        BLOCKING_HEALTH.remove(uuid);

        Vec3 look = player.getLookAngle();
        Vec3 dashVelocity = look.scale(DASH_STRENGTH);
        player.setDeltaMovement(player.getDeltaMovement().add(dashVelocity));
        player.hurtMarked = true;
        player.setOnGround(false);
        player.setSprinting(true);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new ResourceLocation("potionenchant", "sprint")),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        player.fallDistance = 0;
        player.resetFallDistance();

        DASH_AIRBORNE.put(uuid, true);

        super.releaseUsing(stack, level, entity, timeCharged);
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
        return false;
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new DiexvClientItemExtensions() {
            @Override
            public @NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext context) {
                Player player = Minecraft.getInstance().player;
                if (player != null && isSupermode(player.getUUID())) {
                    return DiexvFont3.getFont();
                }
                return Minecraft.getInstance().font;
            }
        });
    }
}
