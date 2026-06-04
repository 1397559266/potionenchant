package net.diexv.potionenchant.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnvilMenu.class, priority = -2147483648)
public class AnvilMenuMixin {

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = false)
    private void onCreateResult(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;

        // 获取输入物品
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();

        // 检查是否是药水附魔
        if (right.getItem() instanceof net.minecraft.world.item.PotionItem) {
            // 对于药水附魔，我们将在事件中处理，这里不做额外处理
            // 这个Mixin主要是为了确保原版逻辑不会干扰我们的固定成本
        }
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void afterCreateResult(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();
        ItemStack output = menu.getSlot(2).getItem();

        // 检查是否是药水附魔
        if (!output.isEmpty() && right.getItem() instanceof net.minecraft.world.item.PotionItem) {
            // 确保修复成本不被增加
            CompoundTag tag = output.getOrCreateTag();
            if (tag.contains("PotionEnchant_OriginalRepairCost")) {
                int originalCost = tag.getInt("PotionEnchant_OriginalRepairCost");
                tag.putInt("RepairCost", originalCost);
            }
        }
    }
}