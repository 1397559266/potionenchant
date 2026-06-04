package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.client.renderer.layers.XArmorPowerLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家渲染器Mixin
 * 为穿着全套X护甲的玩家添加闪电效果层
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, HumanoidModel<AbstractClientPlayer>> {
    
    public PlayerRendererMixin(EntityRendererProvider.Context context, 
                               HumanoidModel<AbstractClientPlayer> model, 
                               float shadowRadius) {
        super(context, model, shadowRadius);
    }
    
    /**
     * 在玩家渲染器构造时注入，添加X套装闪电效果层
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    public void onInit(EntityRendererProvider.Context context, boolean useSlimModel, CallbackInfo ci) {
        // 添加X套装闪电护甲渲染层
        this.addLayer(new XArmorPowerLayer(this, this.getModel()));
    }
}
