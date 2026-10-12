package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class UnpublishCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_PUBLISHED = new SimpleCommandExceptionType(
        Component.translatable("commands.unpublish.notPublished")
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unpublish").requires(Commands.hasPermission(Commands.LEVEL_OWNERS)).executes(c -> unpublish(c.getSource())));
    }

    private static int unpublish(CommandSourceStack source) throws CommandSyntaxException {
        if (!source.getServer().unpublishServer()) {
            throw ERROR_NOT_PUBLISHED.create();
        }

        source.sendSuccess(() -> Component.translatable("commands.unpublish.success"), true);
        return 1;
    }
}
