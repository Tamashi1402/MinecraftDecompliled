package net.minecraft.world.level.blockscan;

import com.google.common.collect.Iterables;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Continuation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;

public class OrderedBlockMatcher extends BlockMatcher {
    private Iterable<BlockPos> positions;

    public OrderedBlockMatcher(LevelReader level, Iterable<BlockPos> positions) {
        super(level);
        this.positions = positions;
    }

    public OrderedBlockMatcher filterPos(Predicate<? super BlockPos> predicate) {
        this.positions = Iterables.filter(this.positions, predicate::test);
        return this;
    }

    public OrderedBlockMatcher filterState(Predicate<BlockState> predicate) {
        super.filterState(predicate);
        return this;
    }

    @Override
    public BlockMatcher filterPosition(java.util.function.BiPredicate<BlockState, BlockPos> predicate) {
        super.filterPosition(predicate);
        return this;
    }

    @Override
    public boolean atLeastMatched(int n) {
        return BlockScanUtils.findBlocksWithCache(this.level, this.positions, this.statePredicate, this.positionPredicate, new BlockStateConsumer() {
            private int count = 0;

            @Override
            public Continuation apply(BlockPos pos, BlockState state) {
                return Continuation.abortIf(++this.count >= n);
            }
        });
    }

    @Override
    protected boolean anyMatched(Predicate<BlockState> statePredicate, java.util.function.BiPredicate<BlockState, BlockPos> positionPredicate) {
        return BlockScanUtils.findBlocksWithCache(this.level, this.positions, statePredicate, positionPredicate, (var0, var1) -> Continuation.ABORT);
    }

    @Override
    public void forEach(BiConsumer<BlockPos, BlockState> consumer) {
        BlockScanUtils.findBlocksWithCache(this.level, this.positions, this.statePredicate, this.positionPredicate, (pos, state) -> {
            consumer.accept(pos, state);
            return Continuation.CONTINUE;
        });
    }

    @Override
    public boolean forEachUntil(BlockStateConsumer consumer) {
        return BlockScanUtils.findBlocksWithCache(this.level, this.positions, this.statePredicate, this.positionPredicate, consumer);
    }

    public Optional<BlockPos> findFirst() {
        MutableObject<Optional<BlockPos>> result = new MutableObject<>(Optional.empty());
        BlockScanUtils.findBlocksWithCache(this.level, this.positions, this.statePredicate, this.positionPredicate, (pos, var2) -> {
            result.setValue(Optional.of(pos.immutable()));
            return Continuation.ABORT;
        });
        return result.get();
    }

    public Optional<BlockPos> findFirst(BiPredicate<BlockPos, BlockState> predicate) {
        MutableObject<Optional<BlockPos>> result = new MutableObject<>(Optional.empty());
        BlockScanUtils.findBlocksWithCache(this.level, this.positions, this.statePredicate, this.positionPredicate, (pos, state) -> {
            if (predicate.test(pos, state)) {
                result.setValue(Optional.of(pos.immutable()));
                return Continuation.ABORT;
            } else {
                return Continuation.CONTINUE;
            }
        });
        return result.get();
    }
}
