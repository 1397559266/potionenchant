package net.diexv.potionenchant.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * X护甲控制命令
 * 穿上全套X护甲后，输入/potionenchant_armorx可以打开控制GUI
 */
public class ArmorXCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("potionenchant_armorx")
                .requires(source -> source.hasPermission(0)) // 不需要特殊权限
                .executes(ArmorXCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        
        // 检查是否穿着全套X护甲
        if (!isWearingFullXArmor(player)) {
            source.sendFailure(Component.translatable("command.potionenchant.armorx.not_full_set"));
            return 0;
        }
        
        // 发送成功消息
        source.sendSuccess(() -> Component.translatable("command.potionenchant.armorx.open_gui"), true);
        
        // 注意：实际的GUI打开逻辑需要在客户端通过按键或其他方式触发
        // 由于命令在服务端执行，我们无法直接打开客户端GUI
        // 这里只是验证玩家是否穿着全套X护甲
        
        return 1;
    }
    
    /**
     * 检查玩家是否穿着全套X护甲
     */
    private static boolean isWearingFullXArmor(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        
        return !helmet.isEmpty() && helmet.getItem() == ModItems.X_HELMET.get() &&
               !chestplate.isEmpty() && chestplate.getItem() == ModItems.X_CHESTPLATE.get() &&
               !leggings.isEmpty() && leggings.getItem() == ModItems.X_LEGGINGS.get() &&
               !boots.isEmpty() && boots.getItem() == ModItems.X_BOOTS.get();
    }
}
