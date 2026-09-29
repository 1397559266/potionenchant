package net.diexv.potionenchant.item;

import net.diexv.potionenchant.client.font.DiexvFont;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import java.util.function.Consumer;

public class UltimatePotionAmulet extends Item implements ICurioItem {

    public UltimatePotionAmulet() {
        super(new Properties()
                .stacksTo(1)
                .rarity(net.minecraft.world.item.Rarity.EPIC)
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // 字体 + 自定义物品渲染器（Forge 官方接缝，平替 ItemRendererMixin）
        consumer.accept(net.diexv.potionenchant.client.CosmicClientItemExtensions.INSTANCE);
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, level, list, flag);
        // 添加饰品标识
        list.add(Component.translatable("tooltip.potionenchant.ultimate_potion_amulet"));
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
