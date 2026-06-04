package net.diexv.potionenchant.client;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.config.ClothConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * 客户端代理类
 * 仅在客户端环境中加载，避免服务端崩溃
 */
@OnlyIn(Dist.CLIENT)
public class ClientProxy {
    
    /**
     * 注册配置屏幕（仅客户端）
     */
    @SuppressWarnings("removal")
    public static void registerConfigScreen() {
        try {
            ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                    (minecraft, screen) -> ClothConfigScreen.createConfigScreen(screen)
                )
            );
        } catch (Exception e) {
            // 静默处理异常
        }
    }
}
