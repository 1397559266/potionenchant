package net.diexv.potionenchant.handlers;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class CustomPotionHudHandler {
    // 这个类现在主要用于物品栏界面的自定义渲染（如果需要）
    // 游戏内HUD已由PotionHudMixin直接处理
}
