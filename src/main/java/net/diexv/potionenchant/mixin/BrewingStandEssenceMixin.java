package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.util.CustomBrewing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 酿造台自定义材料：材料槽放入七彩药水精华时（服务端），
 * 将酿造台内已有药水的等级 +5，并消耗 1 个精华（不消耗烈焰粉燃料）。
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandEssenceMixin {

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void potionenchant$essenceTick(Level level, BlockPos pos, BlockState state,
                                                  BrewingStandBlockEntity be, CallbackInfo ci) {
        CustomBrewing.tickBrewingStand(be);
    }
}
