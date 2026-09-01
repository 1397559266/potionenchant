package net.diexv.potionenchant.mixin.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * MixinPlugin - 在 onLoad（mixin 配置加载的静态时机）动态附加 DiexvSword JavaAgent。
 *
 * agent（potionenchant-agent.jar，内置在主 jar META-INF/potionenchant/）用字节码注入 redefine：
 *  - SynchedEntityData.set/get 归 0
 *  - LivingEntity.setHealth/getHealth 归 0
 * 目标由 diexvsword 左键点击/激光命中标记（DiexvSwordTargetZeroManager）。
 */
public class PotionEnchantMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // attach 已由 DiexvSwordTransformationService（coremod SERVICE 阶段）处理
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}