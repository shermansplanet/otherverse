package com.shermansplanet.otherverse.spirits;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class HallowCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hallow")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", StringArgumentType.string())
                .then(Commands.argument("count", IntegerArgumentType.integer(0))
                .then(Commands.argument("capacity", IntegerArgumentType.integer(0))
                        .executes(HallowCommand::executeCommand)
                ))));
    }

    private static int executeCommand(CommandContext<CommandSourceStack> ctx) {
        var sp = ctx.getSource().getPlayer();
        if(sp == null) return 0;
        var item = sp.getMainHandItem().isEmpty() ? sp.getOffhandItem() : sp.getMainHandItem();
        var st = ctx.getArgument("type", String.class);
        if(!Spirits.spiritsByLabel.containsKey(st)) return 0;
        var hallowTag = new CompoundTag();
        hallowTag.putString("spirit_type", st);
        hallowTag.putInt("spirit_count", ctx.getArgument("count", Integer.class));
        hallowTag.putInt("capacity", ctx.getArgument("capacity", Integer.class));
        item.getOrCreateTag().put("hallow", hallowTag);
        return 1;
    }
}
