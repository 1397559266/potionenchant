package net.diexv.potionenchant.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ClearPotionEnchantCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("clear_potion_enchant")
                .requires(source -> source.hasPermission(2)) // 权限
                .executes(ClearPotionEnchantCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayerOrException();
        ItemStack mainHandItem = player.getMainHandItem();

        if (mainHandItem.isEmpty()) {
            source.sendFailure(Component.literal("您的主手没有物品！"));
            return 0;
        }

        if (!PotionEnchantManager.hasPotionEnchantments(mainHandItem)) {
            source.sendFailure(Component.literal("该物品没有药水附魔！"));
            return 0;
        }

        PotionEnchantManager.clearPotionEnchantments(mainHandItem);
        source.sendSuccess(() -> Component.literal("已成功移除主手物品的药水附魔！"), true);
        return 1;
    }
}
