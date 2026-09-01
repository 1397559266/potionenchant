package net.diexv.potionenchant.item;

import net.diexv.potionenchant.client.DiexvClientItemExtensions;
import net.diexv.potionenchant.client.font.DiexvSwordFont;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class DiexvSwordItem extends Item {

    public DiexvSwordItem() {
        super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.COMMON));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack itemstack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack itemstack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new DiexvClientItemExtensions() {
            @Override
            public @NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                return DiexvSwordFont.getFont();
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, level, list, flag);
        list.add(Component.literal(""));
        list.add(Component.translatable("item.potionenchant.diexv_sword.tooltip.when_in_main_hand"));
        list.add(Component.translatable("item.potionenchant.diexv_sword.tooltip.attack_damage"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
        // 右键开始蓄力（蓄力时长一到自动发射，见 DiexvSwordLaserSkill）
        entity.startUsingItem(hand);
        return super.use(world, entity, hand);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        // 左键点击目标：标记为归零目标（SynchedData 读写/血量全部归 0，由动态附加 mixin 强制）
        // 并立即造成秒杀伤害；持剑玩家自身/其他持剑玩家不会被标记
        if (entity != player && !net.diexv.potionenchant.util.DiexvSwordTargetZeroManager.isSwordBearer(entity)) {
            net.diexv.potionenchant.util.DiexvSwordTargetZeroManager.mark(entity);
        }
        if (entity instanceof LivingEntity livingEntity) {
            DamageSource damageSource = player.damageSources().generic();
            livingEntity.hurt(damageSource, Float.MAX_VALUE);
            ((LivingEntity) entity).setHealth(0.0F);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, net.minecraft.world.entity.LivingEntity entity, int timeLeft) {
        // 松开右键：取消蓄力（不发射）；蓄力满时由 DiexvSwordLaserSkill 自动发射
        if (entity instanceof Player player) {
            net.diexv.potionenchant.entity.DiexvSwordLaserSkill.cancelCharge(player);
        }
        super.releaseUsing(stack, world, entity, timeLeft);
    }
}
