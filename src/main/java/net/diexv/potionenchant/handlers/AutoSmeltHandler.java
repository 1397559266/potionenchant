package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.EnchantmentRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.Tags;

import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class AutoSmeltHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        // 只在服务端执行
        if (level.isClientSide) return;

        // 检查玩家是否使用了带有自动冶炼附魔的工具
        ItemStack tool = player.getMainHandItem();
        int autoSmeltLevel = EnchantmentHelper.getItemEnchantmentLevel(
                EnchantmentRegistry.AUTO_SMELT.get(), tool);

        if (autoSmeltLevel > 0) {
            Block block = event.getState().getBlock();
            ItemStack blockItem = new ItemStack(block);

            // 查找方块的冶炼配方
            Optional<SmeltingRecipe> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                    new net.minecraft.world.SimpleContainer(blockItem), level);

            if (recipe.isPresent()) {
                // 取消原版掉落
                event.setCanceled(true);

                // 移除方块
                level.removeBlock(pos, false);

                // 获取冶炼结果
                ItemStack smeltingResult = recipe.get().getResultItem(level.registryAccess()).copy();

                if (!smeltingResult.isEmpty()) {
                    // 应用时运附魔效果
                    int fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(
                            net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, tool);

                    if (fortuneLevel > 0) {
                        // 时运附魔增加掉落数量
                        int bonusCount = level.random.nextInt(fortuneLevel + 1) + 1;
                        smeltingResult.setCount(smeltingResult.getCount() * bonusCount);
                    }

                    // 生成掉落物
                    Block.popResource(level, pos, smeltingResult);

                    // 给予玩家经验（模拟熔炉经验）
                    if (recipe.get().getExperience() > 0) {
                        int exp = (int) Math.ceil(recipe.get().getExperience());
                        event.getState().getBlock().popExperience((ServerLevel) level, pos, exp);
                    }
                }
            }
        }
    }
}


