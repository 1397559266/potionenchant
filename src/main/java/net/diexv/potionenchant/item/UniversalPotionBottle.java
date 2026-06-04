package net.diexv.potionenchant.item;

import net.diexv.potionenchant.util.font.DiexvFont;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * 万能药水附魔瓶
 * 使用后打开GUI选择药水效果应用到副手物品
 */
public class UniversalPotionBottle extends Item {

    public UniversalPotionBottle(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 始终显示附魔光效
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            // 检查副手是否有物品
            ItemStack offhandItem = player.getOffhandItem();
            if (offhandItem.isEmpty()) {
                player.displayClientMessage(Component.translatable("item.potionenchant.universal_potion_bottle.no_offhand"), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            
            // 打开GUI - 使用延迟加载避免服务端加载客户端类
            openGui(offhandItem, player.getItemInHand(hand));
        }
        
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    
    /**
     * 打开GUI（仅在客户端调用）
     */
    @OnlyIn(Dist.CLIENT)
    private void openGui(ItemStack offhandItem, ItemStack bottleItem) {
        // 这个方法只在客户端被调用，所以可以安全地使用客户端类
        net.minecraft.client.Minecraft.getInstance().setScreen(
            new net.diexv.potionenchant.gui.UniversalPotionBottleScreen(offhandItem, bottleItem)
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull net.minecraft.client.gui.Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                return DiexvFont.getFont();
            }
        });
    }
}
