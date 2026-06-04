package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentDiscoverableMixin {

    @Inject(method = "isDiscoverable", at = @At("RETURN"), cancellable = true)
    private void onIsDiscoverable(CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment)(Object)this;
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(self);
        if (id == null || !PotionEnchantMod.MODID.equals(id.getNamespace())) return;

        if (PotionEnchantConfig.COMMON.discoverableInEnchantingTable.get()) {
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isTreasureOnly", at = @At("RETURN"), cancellable = true)
    private void onIsTreasureOnly(CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment)(Object)this;
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(self);
        if (id == null || !PotionEnchantMod.MODID.equals(id.getNamespace())) return;

        if (PotionEnchantConfig.COMMON.discoverableInEnchantingTable.get()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canApplyAtEnchantingTable", at = @At("RETURN"), cancellable = true, remap = false)
    private void onCanApplyAtEnchantingTable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment)(Object)this;
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(self);
        if (id == null || !PotionEnchantMod.MODID.equals(id.getNamespace())) return;

        if (!PotionEnchantConfig.COMMON.discoverableInEnchantingTable.get()) {
            cir.setReturnValue(false);
        }
    }
}