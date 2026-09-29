package net.diexv.potionenchant.mixin.plugin;

import net.diexv.potionenchant.agent.AgentLauncher;
import net.diexv.potionenchant.agent.DiexvSwordAgent;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * MixinPlugin - 以 onLoad（mixin 配置加载时机）作为 agent 的 loadAgent0 启动时机。
 *
 * agent 启动方式（平移自 DiexvMod AgentLauncher）：不再走外部 attach，
 * 而是反射调用 sun.instrument.InstrumentationImpl.loadAgent0 动态加载，
 * 拿到 Instrumentation 后注册 DiexvSwordAgent（SynchedEntityData.set 归零注入）。
 */
public class PotionEnchantMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        try {
            // loadAgent0 启动（配套工具类 DiexvBase/AgentCallback 平移自 DiexvMod）
            AgentLauncher.start();
            if (AgentLauncher.INST != null) {
                DiexvSwordAgent.install(AgentLauncher.INST);
                System.out.println("[PotionEnchantMixinPlugin] agent 已通过 loadAgent0 启动并注册转换器");
            } else {
                System.err.println("[PotionEnchantMixinPlugin] loadAgent0 未拿到 Instrumentation，agent 未启动");
            }
        } catch (Throwable t) {
            System.err.println("[PotionEnchantMixinPlugin] loadAgent0 启动失败: " + t);
            t.printStackTrace();
        }
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
