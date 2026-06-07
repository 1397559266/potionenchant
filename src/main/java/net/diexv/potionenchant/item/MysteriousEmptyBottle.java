package net.diexv.potionenchant.item;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.font.DiexvFont;
import net.minecraft.client.gui.Font;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class MysteriousEmptyBottle extends Item {

    private static final Random RANDOM = new Random();

    public MysteriousEmptyBottle(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
                return DiexvFont.getFont();
            }
        });
    }

    @SuppressWarnings("removal")
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playLocalSound(player.getX(), player.getY(), player.getZ(), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.PLAYERS, 1.0F, 1.0F, false);

        if (!level.isClientSide) {
            // 获取本模组的所有药水
            List<Potion> modPotions = getAllModPotions();

            if (!modPotions.isEmpty()) {
                // 随机选择一个药水
                Potion selectedPotion = modPotions.get(RANDOM.nextInt(modPotions.size()));

                // 创建药水瓶物品
                ItemStack potionStack = createPotionItem(selectedPotion);

                // 给予玩家药水瓶
                if (!player.getInventory().add(potionStack)) {
                    // 如果背包满了，掉落在地上
                    player.drop(potionStack, false);
                }

                // 消耗物品
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    // 创建药水瓶物品
    private ItemStack createPotionItem(Potion potion) {
        // 随机选择药水类型：普通、喷溅、滞留
        int type = RANDOM.nextInt(3);
        ItemStack potionStack;

        switch (type) {
            case 0:
                potionStack = new ItemStack(Items.SPLASH_POTION);
                break;
            case 1:
                potionStack = new ItemStack(Items.LINGERING_POTION);
                break;
            default:
                potionStack = new ItemStack(Items.POTION);
                break;
        }

        // 设置药水效果
        return PotionUtils.setPotion(potionStack, potion);
    }

    // 获取本模组的所有药水
    private List<Potion> getAllModPotions() {
        List<Potion> potions = new ArrayList<>();

        // 遍历所有注册的药水
        for (Potion potion : ForgeRegistries.POTIONS.getValues()) {
            // 检查药水是否属于本模组
            if (isModPotion(potion)) {
                potions.add(potion);
            }
        }

        return potions;
    }

    // 检查药水是否属于本模组
    private boolean isModPotion(Potion potion) {
        ResourceLocation potionId = ForgeRegistries.POTIONS.getKey(potion);
        return potionId != null && potionId.getNamespace().equals(PotionEnchantMod.MODID);
    }
}
