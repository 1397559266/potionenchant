package net.diexv.potionenchant.item;

import net.diexv.potionenchant.client.font.DiexvFont;
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

public class UniversalEnchantmentBook extends Item {

    public UniversalEnchantmentBook(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ItemStack offhandItem = player.getOffhandItem();
            if (offhandItem.isEmpty()) {
                player.displayClientMessage(
                    Component.translatable("item.potionenchant.universal_enchantment_book.no_offhand"), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            openGui(offhandItem, player.getItemInHand(hand));
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @OnlyIn(Dist.CLIENT)
    private void openGui(ItemStack offhandItem, ItemStack bookItem) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
            new net.diexv.potionenchant.gui.UniversalEnchantmentBookScreen(offhandItem, bookItem)
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
