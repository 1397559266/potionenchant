package net.diexv.potionenchant.util.helper;

import net.minecraft.resources.ResourceLocation;

/**
 * ResourceLocation 兼容性工具类
 * 提供同时兼容 Minecraft 1.20.1 和 1.20.6+ 的 ResourceLocation 创建方法
 * 
 * 注意：由于项目编译目标是 Minecraft 1.20.1，所以内部仍然使用构造函数方式。
 * 当升级到 1.20.6+ 时，只需修改此类的实现即可全局升级。
 */
@SuppressWarnings("deprecation") // 1.20.1 中使用构造函数是正常的
public class ResourceLocationHelper {
    
    /**
     * 使用命名空间和路径创建 ResourceLocation
     * 兼容 1.20.1 和 1.20.6+
     * 
     * @param namespace 命名空间（如模组ID）
     * @param path 路径
     * @return ResourceLocation 实例
     */
    @SuppressWarnings("removal")
    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        // 在 1.20.1 中使用构造函数，在 1.20.6+ 中使用静态方法
        // 由于我们编译目标是 1.20.1，所以直接使用构造函数
        return new ResourceLocation(namespace, path);
    }
    
    /**
     * 使用完整的资源位置字符串创建 ResourceLocation
     * 兼容 1.20.1 和 1.20.6+
     * 
     * @param location 完整的资源位置字符串（如 "minecraft:stone" 或 "modid:path"）
     * @return ResourceLocation 实例
     */

    @SuppressWarnings("removal")
    public static ResourceLocation parse(String location) {
        // 在 1.20.1 中使用构造函数，在 1.20.6+ 中使用静态方法
        // 由于我们编译目标是 1.20.1，所以直接使用构造函数
        return new ResourceLocation(location);
    }
}
