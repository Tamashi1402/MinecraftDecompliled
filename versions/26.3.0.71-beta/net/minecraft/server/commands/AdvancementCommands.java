package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementCommands {
    private static final Dynamic2CommandExceptionType ERROR_CRITERION_NOT_FOUND = new Dynamic2CommandExceptionType(
        (name, criterion) -> Component.translatableEscape("commands.advancement.criterionNotFound", name, criterion)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("advancement")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("grant")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.literal("only")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.ONLY)
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("criterion", StringArgumentType.greedyString())
                                                        .suggests(
                                                            (c, p) -> SharedSuggestionProvider.suggest(
                                                                ResourceKeyArgument.getAdvancement(c, "advancement").value().criteria().keySet(), p
                                                            )
                                                        )
                                                        .executes(
                                                            c -> performCriterion(
                                                                c.getSource(),
                                                                EntityArgument.getPlayers(c, "targets"),
                                                                AdvancementCommands.Action.GRANT,
                                                                ResourceKeyArgument.getAdvancement(c, "advancement"),
                                                                StringArgumentType.getString(c, "criterion")
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("from")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.FROM)
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("until")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.UNTIL)
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("through")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(
                                                            c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.THROUGH
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("everything")
                                        .executes(
                                            c -> perform(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                AdvancementCommands.Action.GRANT,
                                                c.getSource().getServer().getAdvancements().getAllAdvancements(),
                                                false
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("revoke")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.literal("only")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.ONLY)
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("criterion", StringArgumentType.greedyString())
                                                        .suggests(
                                                            (c, p) -> SharedSuggestionProvider.suggest(
                                                                ResourceKeyArgument.getAdvancement(c, "advancement").value().criteria().keySet(), p
                                                            )
                                                        )
                                                        .executes(
                                                            c -> performCriterion(
                                                                c.getSource(),
                                                                EntityArgument.getPlayers(c, "targets"),
                                                                AdvancementCommands.Action.REVOKE,
                                                                ResourceKeyArgument.getAdvancement(c, "advancement"),
                                                                StringArgumentType.getString(c, "criterion")
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("from")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.FROM)
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("until")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.UNTIL)
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("through")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(
                                                            c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.THROUGH
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("everything")
                                        .executes(
                                            c -> perform(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                AdvancementCommands.Action.REVOKE,
                                                c.getSource().getServer().getAdvancements().getAllAdvancements()
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int perform(
        CommandSourceStack source, Collection<ServerPlayer> players, AdvancementCommands.Action action, Collection<AdvancementHolder> advancements
    ) throws CommandSyntaxException {
        return perform(source, players, action, advancements, true);
    }

    private static int perform(
        CommandSourceStack source,
        Collection<ServerPlayer> players,
        AdvancementCommands.Action action,
        Collection<AdvancementHolder> advancements,
        boolean showAdvancements
    ) throws CommandSyntaxException {
        CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

        for (ServerPlayer player : players) {
            tracker.track(player, action.perform(player, advancements, showAdvancements));
        }

        int advancementCount = advancements.size();
        if (advancementCount == 1) {
            AdvancementHolder advancementName = Iterables.getOnlyElement(advancements);
            if (tracker.totalValue() == 0) {
                throw (CommandSyntaxException)tracker.dispatch(CommandResponseTracker.ElementType.ANY, action.singleAdvancementsError, advancementName);
            } else {
                return tracker.sendFeedback(source, true, CommandResponseTracker.ElementType.NON_ZERO, action.singleAdvancementSuccessResponse, advancementName);
            }
        } else if (tracker.totalValue() == 0) {
            throw (CommandSyntaxException)tracker.dispatch(CommandResponseTracker.ElementType.ANY, action.multipleAdvancementsError, advancementCount);
        } else {
            return tracker.sendFeedback(source, true, CommandResponseTracker.ElementType.NON_ZERO, action.multipleAdvancementsSuccessResponse, advancementCount);
        }
    }

    private static int performCriterion(
        CommandSourceStack source, Collection<ServerPlayer> players, AdvancementCommands.Action action, AdvancementHolder holder, String criterion
    ) throws CommandSyntaxException {
        Advancement advancement = holder.value();
        if (!advancement.criteria().containsKey(criterion)) {
            throw ERROR_CRITERION_NOT_FOUND.create(Advancement.name(holder), criterion);
        }

        CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

        for (ServerPlayer player : players) {
            tracker.track(player, action.performCriterion(player, holder, criterion));
        }

        if (tracker.totalValue() == 0) {
            throw (CommandSyntaxException)tracker.dispatch(CommandResponseTracker.ElementType.ANY, action.criterionError, holder, criterion);
        } else {
            return tracker.sendFeedback(source, true, CommandResponseTracker.ElementType.NON_ZERO, action.criterionSuccessResponse, holder, criterion);
        }
    }

    private static List<AdvancementHolder> getAdvancements(CommandContext<CommandSourceStack> context, AdvancementHolder target, AdvancementCommands.Mode mode) {
        AdvancementTree advancementTree = context.getSource().getServer().getAdvancements().tree();
        AdvancementNode targetNode = advancementTree.get(target);
        if (targetNode == null) {
            return List.of(target);
        }

        List<AdvancementHolder> advancements = new ArrayList<>();
        if (mode.parents) {
            for (AdvancementNode parent = targetNode.parent(); parent != null; parent = parent.parent()) {
                advancements.add(parent.holder());
            }
        }

        advancements.add(target);
        if (mode.children) {
            addChildren(targetNode, advancements);
        }

        return advancements;
    }

    private static void addChildren(AdvancementNode parent, List<AdvancementHolder> output) {
        for (AdvancementNode child : parent.children()) {
            output.add(child.holder());
            addChildren(child, output);
        }
    }

    private enum Action {
        GRANT("grant") {
            @Override
            protected boolean perform(ServerPlayer player, AdvancementHolder advancement) {
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                if (progress.isDone()) {
                    return false;
                }

                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }

                return true;
            }

            @Override
            protected boolean performCriterion(ServerPlayer player, AdvancementHolder advancement, String criterion) {
                return player.getAdvancements().award(advancement, criterion);
            }
        },
        REVOKE("revoke") {
            @Override
            protected boolean perform(ServerPlayer player, AdvancementHolder advancement) {
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                if (!progress.hasProgress()) {
                    return false;
                }

                for (String criterion : progress.getCompletedCriteria()) {
                    player.getAdvancements().revoke(advancement, criterion);
                }

                return true;
            }

            @Override
            protected boolean performCriterion(ServerPlayer player, AdvancementHolder advancement, String criterion) {
                return player.getAdvancements().revoke(advancement, criterion);
            }
        };

        public final CommandResponseTracker.MessagesWithArg<ServerPlayer, AdvancementHolder> singleAdvancementSuccessResponse;
        public final CommandResponseTracker.DispatchWithArg<CommandSyntaxException, ServerPlayer, AdvancementHolder> singleAdvancementsError;
        public final CommandResponseTracker.MessagesWithArg<ServerPlayer, Integer> multipleAdvancementsSuccessResponse;
        public final CommandResponseTracker.DispatchWithArg<CommandSyntaxException, ServerPlayer, Integer> multipleAdvancementsError;
        public final CommandResponseTracker.MessagesWithArgs<ServerPlayer, AdvancementHolder, String> criterionSuccessResponse;
        public final CommandResponseTracker.DispatchWithArgs<CommandSyntaxException, ServerPlayer, AdvancementHolder, String> criterionError;

        Action(String key) {
            this.singleAdvancementSuccessResponse = CommandResponseTracker.messages(
                (player, var2, advancement) -> Component.translatable(
                    "commands.advancement." + key + ".one.to.one.success", Advancement.name(advancement), player.getDisplayName()
                ),
                (playerCount, var2, advancement) -> Component.translatable(
                    "commands.advancement." + key + ".one.to.many.success", Advancement.name(advancement), playerCount
                )
            );
            Dynamic2CommandExceptionType singleAdvancementSinglePlayerError = new Dynamic2CommandExceptionType(
                (advancement, player) -> Component.translatableEscape("commands.advancement." + key + ".one.to.one.failure", advancement, player)
            );
            Dynamic2CommandExceptionType singleAdvancementMultiplePlayersError = new Dynamic2CommandExceptionType(
                (advancement, playerCount) -> Component.translatableEscape("commands.advancement." + key + ".one.to.many.failure", advancement, playerCount)
            );
            this.singleAdvancementsError = new CommandResponseTracker.DispatchWithArg<>(
                (player, var2, advancement) -> singleAdvancementSinglePlayerError.create(Advancement.name(advancement), player.getDisplayName()),
                (playerCount, var2, advancement) -> singleAdvancementMultiplePlayersError.create(Advancement.name(advancement), playerCount)
            );
            this.multipleAdvancementsSuccessResponse = CommandResponseTracker.messages(
                (player, var2, advancementCount) -> Component.translatable(
                    "commands.advancement." + key + ".many.to.one.success", advancementCount, player.getDisplayName()
                ),
                (playerCount, var2, advancementCount) -> Component.translatable(
                    "commands.advancement." + key + ".many.to.many.success", advancementCount, playerCount
                )
            );
            Dynamic2CommandExceptionType multipleAdvancementSinglePlayerError = new Dynamic2CommandExceptionType(
                (advancementCount, player) -> Component.translatableEscape("commands.advancement." + key + ".many.to.one.failure", advancementCount, player)
            );
            Dynamic2CommandExceptionType multipleAdvancementMultiplePlayersError = new Dynamic2CommandExceptionType(
                (advancementCount, playerCount) -> Component.translatableEscape(
                    "commands.advancement." + key + ".many.to.many.failure", advancementCount, playerCount
                )
            );
            this.multipleAdvancementsError = new CommandResponseTracker.DispatchWithArg<>(
                (player, var2, advancementCount) -> multipleAdvancementSinglePlayerError.create(advancementCount, player.getDisplayName()),
                (playerCount, var2, advancementCount) -> multipleAdvancementMultiplePlayersError.create(advancementCount, playerCount)
            );
            this.criterionSuccessResponse = CommandResponseTracker.messages(
                (player, var2, advancement, criterion) -> Component.translatable(
                    "commands.advancement." + key + ".criterion.to.one.success", criterion, Advancement.name(advancement), player.getDisplayName()
                ),
                (playerCount, var2, advancement, criterion) -> Component.translatable(
                    "commands.advancement." + key + ".criterion.to.many.success", criterion, Advancement.name(advancement), playerCount
                )
            );
            Dynamic3CommandExceptionType criterionSinglePlayerError = new Dynamic3CommandExceptionType(
                (criterion, advancement, player) -> Component.translatableEscape(
                    "commands.advancement." + key + ".criterion.to.one.failure", criterion, advancement, player
                )
            );
            Dynamic3CommandExceptionType criterionMultiplePlayersError = new Dynamic3CommandExceptionType(
                (criterion, advancement, playerCount) -> Component.translatableEscape(
                    "commands.advancement." + key + ".criterion.to.many.failure", criterion, advancement, playerCount
                )
            );
            this.criterionError = new CommandResponseTracker.DispatchWithArgs<>(
                (player, var2, advancement, criterion) -> criterionSinglePlayerError.create(criterion, Advancement.name(advancement), player.getDisplayName()),
                (playerCount, var2, advancement, criterion) -> criterionMultiplePlayersError.create(criterion, Advancement.name(advancement), playerCount)
            );
        }

        public int perform(ServerPlayer player, Iterable<AdvancementHolder> advancements, boolean showAdvancements) {
            int count = 0;
            if (!showAdvancements) {
                player.getAdvancements().flushDirty(player, true);
            }

            for (AdvancementHolder advancement : advancements) {
                if (this.perform(player, advancement)) {
                    count++;
                }
            }

            if (!showAdvancements) {
                player.getAdvancements().flushDirty(player, false);
            }

            return count;
        }

        protected abstract boolean perform(ServerPlayer player, AdvancementHolder advancement);

        protected abstract boolean performCriterion(ServerPlayer player, AdvancementHolder advancement, String criterion);
    }

    private enum Mode {
        ONLY(false, false),
        THROUGH(true, true),
        FROM(false, true),
        UNTIL(true, false),
        EVERYTHING(true, true);

        private final boolean parents;
        private final boolean children;

        Mode(boolean parents, boolean children) {
            this.parents = parents;
            this.children = children;
        }
    }
}
