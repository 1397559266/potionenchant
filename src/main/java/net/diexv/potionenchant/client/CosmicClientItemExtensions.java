package net.diexv.potionenchant.client;

import net.diexv.potionenchant.client.font.DiexvFont;
import net.diexv.potionenchant.client.renderer.CosmicItemRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * 全模组「带着色器物品」共用的客户端扩展。
 *
 * <p>只做两件事：默认字体（DiexvFont）+ 自定义物品渲染器
 * （{@link CosmicItemRenderer}，即原 {@code ItemRendererMixin} 的官方接缝平替）。
 * 需要不同字体（如 DiexvSword 用 DiexvSwordFont、XSword 超模式用 DiexvFont3）
 * 或需要 BLOCK 手臂姿态的物品，请改继承 {@link DiexvClientItemExtensions} 并覆写。
 */
@OnlyIn(Dist.CLIENT)
public class CosmicClientItemExtensions implements IClientItemExtensions {

    public static final CosmicClientItemExtensions INSTANCE = new CosmicClientItemExtensions();

    @Override
    public @NotNull Font getFont(ItemStack stack, FontContext context) {
        return DiexvFont.getFont();
    }

    @Override
    public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return CosmicItemRenderer.get();
    }
}
