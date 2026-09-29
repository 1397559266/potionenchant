package net.diexv.potionenchant.craft;

import java.util.ArrayList;
import java.util.List;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 七彩药水精华配方：
 * 3x3 工作台 —— 正中心必须是空玻璃瓶；
 * 玻璃瓶正上方必须是烈焰粉；
 * 其余 7 个外圈格分别为：金锭、钻石、绿宝石、红石、青金石、萤石、
 * 紫水晶碎片或紫水晶簇（二选一），7 种材料摆放位置不强制。
 */
public class RainbowPotionEssenceRecipe extends CustomRecipe {

    public static final RecipeSerializer<RainbowPotionEssenceRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(RainbowPotionEssenceRecipe::new);

    private static final Item GLASS_BOTTLE = Items.GLASS_BOTTLE;
    private static final Item BLAZE_POWDER = Items.BLAZE_POWDER;
    private static final Item[] RING = {
            Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD,
            Items.REDSTONE, Items.LAPIS_LAZULI, Items.GLOWSTONE_DUST
    };

    public RainbowPotionEssenceRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    /** 外圈格子属于哪种材料：0..5 = 六种固定材料，6 = 紫水晶（碎片/簇皆可），-1 = 不匹配 */
    private static int kindOf(Item item) {
        for (int i = 0; i < RING.length; i++) {
            if (item == RING[i]) return i;
        }
        // 紫水晶系：碎片 / 簇 / 小中大芽皆可（块除外）
        if (item == Items.AMETHYST_SHARD || item == Items.AMETHYST_CLUSTER
                || item == Items.SMALL_AMETHYST_BUD || item == Items.MEDIUM_AMETHYST_BUD
                || item == Items.LARGE_AMETHYST_BUD) return 6;
        return -1;
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;
        // 中心 = 玻璃瓶；中心上方(index 1) = 烈焰粉
        if (inv.getItem(4).getItem() != GLASS_BOTTLE) return false;
        if (inv.getItem(1).getItem() != BLAZE_POWDER) return false;
        List<Item> ring = new ArrayList<>(7);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (i == 1 || i == 4) continue;
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) return false; // 其余 7 格必须填满
            ring.add(s.getItem());
        }
        boolean[] used = new boolean[7];
        for (Item item : ring) {
            int kind = kindOf(item);
            if (kind < 0 || used[kind]) return false; // 非法或重复材料
            used[kind] = true;
        }
        for (boolean u : used) {
            if (!u) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        return new ItemStack(ModItems.RAINBOW_POTION_ESSENCE.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
