package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 战利品表事件处理器
 * 动态为所有战利品表添加终极药水护符的生成机会
 */
@Mod.EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = "potionenchant")
public class LootTableEventHandler {
    
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        // 获取配置的生成概率
        int lootChance = PotionEnchantConfig.SERVER.ultimatePotionAmuletLootChance.get();
        
        // 如果概率为0,不添加
        if (lootChance <= 0) {
            return;
        }
        
        String tableName = event.getName().toString();
        
        // 检查是否是合适的战利品表类型
        if (!isAppropriateLootTable(tableName)) {
            return;
        }
        
        // 直接添加战利品池，使用配置的概率作为权重
        // 这样每次生成战利品时都会独立判断
        addAmuletToLootTable(event, lootChance);
    }
    
    /**
     * 判断是否是合适的战利品表
     * 兼容原版和非原版的战利品表
     */
    private static boolean isAppropriateLootTable(String tableName) {
        // 排除一些不应该添加的地方
        if (tableName.contains("fishing") ||           // 钓鱼
            tableName.contains("entity") ||             // 生物掉落
            tableName.contains("mob") ||                // 怪物
            tableName.contains("gameplay")) {           // 游戏玩法
            return false;
        }
        
        // 允许所有宝箱类型的战利品表
        // 包括原版和各种模组的chest战利品表
        return tableName.contains("chests/") || 
               tableName.contains("chest") ||
               tableName.contains("structures/");
    }
    
    /**
     * 向战利品表中添加终极药水护符
     * @param lootChance 配置的概率值 (0-100)，用作权重
     */
    private static void addAmuletToLootTable(LootTableLoadEvent event, int lootChance) {
        // 创建一个新的战利品池
        // 使用权重系统实现概率控制：
        // - 护符的权重 = lootChance
        // - 空项的权重 = 100 - lootChance
        // 这样每次生成时，获得护符的概率 = lootChance / 100 = lootChance%
        
        LootPool.Builder poolBuilder = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1));
        
        if (lootChance >= 100) {
            // 如果概率为100%，只添加护符，不添加空项
            poolBuilder.add(LootItem.lootTableItem(ModItems.ULTIMATE_POTION_AMULET.get())
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));
        } else if (lootChance > 0) {
            // 正常情况：使用权重系统
            int emptyWeight = 100 - lootChance;
            poolBuilder.add(LootItem.lootTableItem(ModItems.ULTIMATE_POTION_AMULET.get())
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                    .setWeight(lootChance))
                    .add(EmptyLootItem.emptyItem().setWeight(emptyWeight));
        }
        
        // 添加到战利品表
        event.getTable().addPool(poolBuilder.build());
    }
}
