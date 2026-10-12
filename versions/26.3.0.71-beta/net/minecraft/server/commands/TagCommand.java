package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.Entity;

public class TagCommand {
    private static final SimpleCommandExceptionType ERROR_ADD_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.tag.add.failed"));
    private static final SimpleCommandExceptionType ERROR_REMOVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.tag.remove.failed"));
    private static final CommandResponseTracker.Messages<Entity> RESPONSE_NO_TAGS = CommandResponseTracker.messages(
        (entity, var1) -> Component.translatable("commands.tag.list.single.empty", entity.getDisplayName()),
        (entityCount, var1) -> Component.translatable("commands.tag.list.multiple.empty", entityCount)
    );
    private static final CommandResponseTracker.MessagesWithArg<Entity, String> RESPONSE_REMOVE = CommandResponseTracker.messages(
        ERROR_REMOVE_FAILED,
        (entity, var1, name) -> Component.translatable("commands.tag.remove.success.single", name, entity.getDisplayName()),
        (entityCount, var1, name) -> Component.translatable("commands.tag.remove.success.multiple", name, entityCount)
    );
    private static final CommandResponseTracker.MessagesWithArg<Entity, String> RESPONSE_ADD = CommandResponseTracker.messages(
        ERROR_ADD_FAILED,
        (entity, var1, name) -> Component.translatable("commands.tag.add.success.single", name, entity.getDisplayName()),
        (entityCount, var1, name) -> Component.translatable("commands.tag.add.success.multiple", name, entityCount)
    );
    private static final CommandResponseTracker.MessagesWithArg<Entity, Set<String>> RESPONSE_LIST = CommandResponseTracker.messages(
        (entity, var1, tags) -> Component.translatable(
            "commands.tag.list.single.success", entity.getDisplayName(), tags.size(), ComponentUtils.formatList(tags)
        ),
        (entityCount, var1, tags) -> Component.translatable("commands.tag.list.multiple.success", entityCount, tags.size(), ComponentUtils.formatList(tags))
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("tag")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.argument("targets", EntityArgument.entities())
                        .then(
                            Commands.literal("add")
                                .then(
                                    Commands.argument("name", StringArgumentType.word())
                                        .executes(c -> addTag(c.getSource(), EntityArgument.getEntities(c, "targets"), StringArgumentType.getString(c, "name")))
                                )
                        )
                        .then(
                            Commands.literal("remove")
                                .then(
                                    Commands.argument("name", StringArgumentType.word())
                                        .suggests((c, p) -> SharedSuggestionProvider.suggest(getTags(EntityArgument.getEntities(c, "targets")), p))
                                        .executes(
                                            c -> removeTag(c.getSource(), EntityArgument.getEntities(c, "targets"), StringArgumentType.getString(c, "name"))
                                        )
                                )
                        )
                        .then(Commands.literal("list").executes(c -> listTags(c.getSource(), EntityArgument.getEntities(c, "targets"))))
                )
        );
    }

    private static Collection<String> getTags(Collection<? extends Entity> entities) {
        Set<String> result = new HashSet<>();

        for (Entity entity : entities) {
            result.addAll(entity.entityTags());
        }

        return result;
    }

    private static int addTag(CommandSourceStack source, Collection<? extends Entity> targets, String name) throws CommandSyntaxException {
        CommandResponseTracker<Entity> response = CommandResponseTracker.create();

        for (Entity entity : targets) {
            response.track(entity, entity.addTag(name));
        }

        return response.sendFeedback(source, true, RESPONSE_ADD, name);
    }

    private static int removeTag(CommandSourceStack source, Collection<? extends Entity> targets, String name) throws CommandSyntaxException {
        CommandResponseTracker<Entity> response = CommandResponseTracker.create();

        for (Entity entity : targets) {
            response.track(entity, entity.removeTag(name));
        }

        return response.sendFeedback(source, true, RESPONSE_REMOVE, name);
    }

    private static int listTags(CommandSourceStack source, Collection<? extends Entity> targets) throws CommandSyntaxException {
        CommandResponseTracker<Entity> response = CommandResponseTracker.create();
        Set<String> tags = new HashSet<>();

        for (Entity entity : targets) {
            Set<String> entityTags = entity.entityTags();
            tags.addAll(entityTags);
            response.track(entity, entityTags.size());
        }

        if (tags.isEmpty()) {
            response.sendFeedback(source, false, RESPONSE_NO_TAGS);
        } else {
            response.sendFeedback(source, false, RESPONSE_LIST, tags);
        }

        return tags.size();
    }
}
