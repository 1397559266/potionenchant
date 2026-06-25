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

        // isDiscoverable is used by both enchanting table display and chest loot (EnchantRandomlyFunction)
        // Return true if either source is enabled
        boolean chestLoot = PotionEnchantConfig.SERVER.enchantBookChestLoot.get();
        boolean enchantTable = PotionEnchantConfig.SERVER.discoverableInEnchantingTable.get();
        cir.setReturnValue(chestLoot || enchantTable);
    }

    @Inject(method = "isTreasureOnly", at = @At("RETURN"), cancellable = true)
    private void onIsTreasureOnly(CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment)(Object)this;
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(self);
        if (id == null || !PotionEnchantMod.MODID.equals(id.getNamespace())) return;

        // isTreasureOnly is used by EnchantRandomlyFunction as: isDiscoverable() && !isTreasureOnly()
        // When chest loot is disabled but enchanting table is enabled, isDiscoverable() returns true,
        // so we must set isTreasureOnly() to true to block chest loot.
        boolean chestLoot = PotionEnchantConfig.SERVER.enchantBookChestLoot.get();
        boolean enchantTable = PotionEnchantConfig.SERVER.discoverableInEnchantingTable.get();
        if (!chestLoot && enchantTable) {
            cir.setReturnValue(true);
        } else if (!chestLoot && !enchantTable) {
            // isDiscoverable already returns false, so treasure-only doesn't affect chest loot.
            // But still mark as treasure to prevent accidental generation.
            cir.setReturnValue(true);
        } else if (chestLoot && !enchantTable) {
            // isDiscoverable returns true (due to chestLoot), treasure-only should be false
            cir.setReturnValue(false);
        }
        // else (both true): let default behavior, don't force change
    }

    @Inject(method = "isTradeable", at = @At("RETURN"), cancellable = true)
    private void onIsTradeable(CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment)(Object)this;
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(self);
        if (id == null || !PotionEnchantMod.MODID.equals(id.getNamespace())) return;

        // isTradeable controls whether villagers can offer enchanted books with this enchantment
        if (!PotionEnchantConfig.SERVER.enchantBookVillagerTrades.get()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canApplyAtEnchantingTable", at = @At("RETURN"), cancellable = true, remap = false)
    private void onCanApplyAtEnchantingTable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment)(Object)this;
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(self);
        if (id == null || !PotionEnchantMod.MODID.equals(id.getNamespace())) return;

        if (!PotionEnchantConfig.SERVER.discoverableInEnchantingTable.get()) {
            cir.setReturnValue(false);
        }
    }
}
