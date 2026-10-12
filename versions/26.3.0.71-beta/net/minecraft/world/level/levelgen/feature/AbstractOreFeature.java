package net.minecraft.world.level.levelgen.feature;

import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractOreFeature implements Feature {
    protected final List<BlockReplacement> targetStates;
    protected final int size;
    protected final float discardChanceOnAirExposure;

    public AbstractOreFeature(List<BlockReplacement> targetStates, int size, float discardChanceOnAirExposure) {
        this.targetStates = targetStates;
        this.size = size;
        this.discardChanceOnAirExposure = discardChanceOnAirExposure;
    }

    protected static <T extends AbstractOreFeature> MapCodec<T> makeCodec(Function3<List<BlockReplacement>, Integer, Float, T> constructor) {
        return RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.list(BlockReplacement.CODEC).fieldOf("targets").forGetter(AbstractOreFeature::targetStates),
                    Codec.intRange(0, 64).fieldOf("size").forGetter(AbstractOreFeature::size),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("discard_chance_on_air_exposure").forGetter(AbstractOreFeature::discardChanceOnAirExposure)
                )
                .apply(i, constructor)
        );
    }

    @Override
    public abstract MapCodec<? extends AbstractOreFeature> codec();

    public final List<BlockReplacement> targetStates() {
        return this.targetStates;
    }

    public final int size() {
        return this.size;
    }

    public final float discardChanceOnAirExposure() {
        return this.discardChanceOnAirExposure;
    }

    public boolean canPlaceOre(
        BlockState state, Function<BlockPos, BlockState> blockGetter, RandomSource random, BlockReplacement targetState, BlockPos.MutableBlockPos orePos
    ) {
        if (!targetState.target().test(state, orePos, random)) {
            return false;
        } else {
            return shouldSkipAirCheck(random, this.discardChanceOnAirExposure) ? true : !isAdjacentToAir(blockGetter, orePos);
        }
    }

    public static boolean isAdjacentToAir(Function<BlockPos, BlockState> blockGetter, BlockPos pos) {
        return checkNeighbors(blockGetter, pos, BlockBehaviour.BlockStateBase::isAir);
    }

    public static boolean checkNeighbors(Function<BlockPos, BlockState> blockGetter, BlockPos pos, Predicate<BlockState> predicate) {
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.values()) {
            neighborPos.setWithOffset(pos, direction);
            if (predicate.test(blockGetter.apply(neighborPos))) {
                return true;
            }
        }

        return false;
    }

    private static boolean shouldSkipAirCheck(RandomSource random, float discardChanceOnAirExposure) {
        if (discardChanceOnAirExposure <= 0.0F) {
            return true;
        } else {
            return discardChanceOnAirExposure >= 1.0F ? false : random.nextFloat() >= discardChanceOnAirExposure;
        }
    }
}
