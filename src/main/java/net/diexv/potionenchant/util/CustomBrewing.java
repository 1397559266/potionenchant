package net.diexv.potionenchant.util;

import java.util.ArrayList;
import java.util.List;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/**
 * 酿造台自定义酿造：材料槽为七彩药水精华时，把三个药水输入槽内
 * 所有已有药水效果等级 +5，并消耗 1 个精华（不消耗烈焰粉燃料）。
 */
public final class CustomBrewing {

    private CustomBrewing() {
    }

    public static void tickBrewingStand(BrewingStandBlockEntity be) {
        if (be == null) return;
        Level level = be.getLevel();
        if (level == null || level.isClientSide) return;
        ItemStack ingredient = be.getItem(3);
        if (ingredient.isEmpty() || ingredient.getItem() != ModItems.RAINBOW_POTION_ESSENCE.get()) return;

        boolean any = false;
        for (int i = 0; i < 3; i++) {
            ItemStack input = be.getItem(i);
            if (!input.isEmpty() && boostStack(input, 5)) {
                any = true;
            }
        }
        if (any) {
            ingredient.shrink(1);
            be.setChanged();
        }
    }

    /** 把一瓶药水的所有效果等级 +levels（上限 255）；无效果（水）不处理 */
    private static boolean boostStack(ItemStack stack, int levels) {
        List<MobEffectInstance> fx = PotionUtils.getMobEffects(stack);
        if (fx.isEmpty()) return false;
        List<MobEffectInstance> boosted = new ArrayList<>(fx.size());
        for (MobEffectInstance e : fx) {
            int amp = e.getAmplifier() + levels;
            boosted.add(new MobEffectInstance(e.getEffect(), e.getDuration(), amp, e.isAmbient(), e.isVisible()));
        }
        // 移除原版 Potion 与自定义颜色，改为写自定义效果（保证读到的就是提升后的效果）
        stack.removeTagKey("Potion");
        stack.removeTagKey("CustomPotionColor");
        PotionUtils.setCustomEffects(stack, boosted);
        return true;
    }
}
