package net.minecraft.gametest.framework;

import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;

public class TestFinder implements TestInstanceFinder, TestPosFinder {
    private static final TestInstanceFinder NO_FUNCTIONS = Stream::empty;
    private static final TestPosFinder NO_STRUCTURES = Stream::empty;
    private final TestInstanceFinder testInstanceFinder;
    private final TestPosFinder testPosFinder;
    private final CommandSourceStack source;

    @Override
    public Stream<GlobalPos> findTestPos() {
        return this.testPosFinder.findTestPos();
    }

    public static TestFinder.Builder builder() {
        return new TestFinder.Builder();
    }

    private TestFinder(CommandSourceStack source, TestInstanceFinder testInstanceFinder, TestPosFinder testPosFinder) {
        this.source = source;
        this.testInstanceFinder = testInstanceFinder;
        this.testPosFinder = testPosFinder;
    }

    public CommandSourceStack source() {
        return this.source;
    }

    @Override
    public Stream<Holder.Reference<GameTestInstance>> findTests() {
        return this.testInstanceFinder.findTests();
    }

    public static class Builder {
        private final UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> testFinderWrapper;
        private final UnaryOperator<Supplier<Stream<GlobalPos>>> structureBlockPosFinderWrapper;

        public Builder() {
            this.testFinderWrapper = f -> f;
            this.structureBlockPosFinderWrapper = f -> f;
        }

        private Builder(
            UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> testFinderWrapper,
            UnaryOperator<Supplier<Stream<GlobalPos>>> structureBlockPosFinderWrapper
        ) {
            this.testFinderWrapper = testFinderWrapper;
            this.structureBlockPosFinderWrapper = structureBlockPosFinderWrapper;
        }

        public TestFinder.Builder createMultipleCopies(int amount) {
            return new TestFinder.Builder(createCopies(amount), createCopies(amount));
        }

        private static <Q> UnaryOperator<Supplier<Stream<Q>>> createCopies(int amount) {
            return source -> {
                List<Q> copyList = new LinkedList<>();
                List<Q> sourceList = ((Stream)source.get()).toList();

                for (int i = 0; i < amount; i++) {
                    copyList.addAll(sourceList);
                }

                return copyList::stream;
            };
        }

        private TestFinder build(CommandSourceStack source, TestInstanceFinder testInstanceFinder, TestPosFinder testPosFinder) {
            List<Holder.Reference<GameTestInstance>> selectedTests = testInstanceFinder.findTests().toList();
            return new TestFinder(
                source,
                this.testFinderWrapper.apply(selectedTests::stream)::get,
                () -> this.structureBlockPosFinderWrapper.apply(testPosFinder::findTestPos).get()
            );
        }

        public TestFinder radius(CommandContext<CommandSourceStack> sourceStack, int radius) {
            CommandSourceStack source = sourceStack.getSource();
            BlockPos pos = BlockPos.containing(source.getPosition());
            return this.build(
                source,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.findTestBlocks(pos, radius, source.getLevel()).map(p -> new GlobalPos(source.getLevel().dimension(), p))
            );
        }

        public TestFinder nearest(CommandContext<CommandSourceStack> sourceStack) {
            CommandSourceStack source = sourceStack.getSource();
            BlockPos pos = BlockPos.containing(source.getPosition());
            return this.build(
                source,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.findNearestTest(pos, 15, source.getLevel()).map(p -> new GlobalPos(source.getLevel().dimension(), p)).stream()
            );
        }

        public TestFinder allNearby(CommandContext<CommandSourceStack> sourceStack) {
            CommandSourceStack source = sourceStack.getSource();
            BlockPos pos = BlockPos.containing(source.getPosition());
            return this.build(
                source,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.findTestBlocks(pos, 250, source.getLevel()).map(p -> new GlobalPos(source.getLevel().dimension(), p))
            );
        }

        public TestFinder lookedAt(CommandContext<CommandSourceStack> sourceStack) {
            CommandSourceStack source = sourceStack.getSource();
            return this.build(
                source,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.lookedAtTestPos(BlockPos.containing(source.getPosition()), source.getPlayer().getCamera(), source.getLevel())
                    .map(p -> new GlobalPos(source.getLevel().dimension(), p))
            );
        }

        public TestFinder failedTests(CommandContext<CommandSourceStack> sourceStack, boolean onlyRequiredTests) {
            return this.build(
                sourceStack.getSource(),
                () -> FailedTestTracker.getLastFailedTests().filter(test -> !onlyRequiredTests || test.value().required()),
                TestFinder.NO_STRUCTURES
            );
        }

        public TestFinder byResourceSelection(CommandContext<CommandSourceStack> sourceStack, Collection<Holder.Reference<GameTestInstance>> holders) {
            return this.build(sourceStack.getSource(), holders::stream, TestFinder.NO_STRUCTURES);
        }

        public TestFinder failedTests(CommandContext<CommandSourceStack> sourceStack) {
            return this.failedTests(sourceStack, false);
        }
    }
}
