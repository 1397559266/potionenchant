package net.diexv.potionenchant.client;

import net.diexv.potionenchant.gui.ArmorXControlScreen;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * X护甲控制按键处理器
 * 按X键打开X护甲控制GUI（需穿着全套X护甲）
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class ArmorXKeyHandler {

    public static KeyMapping ARMOR_X_CONTROL_KEY;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        ARMOR_X_CONTROL_KEY = new KeyMapping(
                "key.potionenchant.armorx_control", // 语言键
                GLFW.GLFW_KEY_X,                   // 默认键位：X键
                "category.potionenchant.armorx"    // 键位类别
        );
        event.register(ARMOR_X_CONTROL_KEY);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (ARMOR_X_CONTROL_KEY.isDown()) {
                onArmorXKeyPressed();
            }
        }
    }

    /**
     * 处理X护甲控制按键按下事件
     */
    private static void onArmorXKeyPressed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) { // 确保没有打开其他GUI
            // 检查是否穿着全套X护甲
            if (isWearingFullXArmor(mc.player)) {
                // 打开GUI
                mc.setScreen(new ArmorXControlScreen());
            }
        }
    }

    /**
     * 检查玩家是否穿着全套X护甲
     */
    private static boolean isWearingFullXArmor(net.minecraft.client.player.AbstractClientPlayer player) {
        net.minecraft.world.item.ItemStack helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        net.minecraft.world.item.ItemStack chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        net.minecraft.world.item.ItemStack leggings = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        net.minecraft.world.item.ItemStack boots = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);

        return !helmet.isEmpty() && helmet.getItem() == ModItems.X_HELMET.get() &&
               !chestplate.isEmpty() && chestplate.getItem() == ModItems.X_CHESTPLATE.get() &&
               !leggings.isEmpty() && leggings.getItem() == ModItems.X_LEGGINGS.get() &&
               !boots.isEmpty() && boots.getItem() == ModItems.X_BOOTS.get();
    }
}
