package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class KillCommand {
    private static final CommandResponseTracker.Messages<Entity> RESPONSE_KILL = CommandResponseTracker.messages(
        (entity, var1) -> Component.translatable("commands.kill.success.single", entity.getDisplayName()),
        (entityCount, var1) -> Component.translatable("commands.kill.success.multiple", entityCount)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("kill")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> kill(c.getSource(), ImmutableList.of(c.getSource().getEntityOrException())))
                .then(Commands.argument("targets", EntityArgument.entities()).executes(c -> kill(c.getSource(), EntityArgument.getEntities(c, "targets"))))
        );
    }

    private static int kill(CommandSourceStack source, Collection<? extends Entity> victims) throws CommandSyntaxException {
        CommandResponseTracker<Entity> tracker = CommandResponseTracker.create();

        for (Entity entity : victims) {
            entity.kill(source.getLevel());
            tracker.track(entity);
        }

        return tracker.sendFeedback(source, true, RESPONSE_KILL);
    }
}
