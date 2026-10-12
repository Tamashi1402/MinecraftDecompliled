package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MushroomBlock extends VegetationBlock implements BonemealableBlock {
    private static final int SEARCH_RADIUS = 4;
    private static final int MAX_NEIGHBORS_TO_GROW = 4;
    private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 6.0);
    private final ResourceKey<Feature> feature;

    public MushroomBlock(ResourceKey<Feature> feature, BlockBehaviour.Properties properties) {
        super(properties);
        this.feature = feature;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(25) == 0) {
            if (!this.canSpread(level, pos)) {
                return;
            }

            BlockPos offset = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);

            for (int i = 0; i < 4; i++) {
                if (level.isEmptyBlock(offset) && state.canSurvive(level, offset)) {
                    pos = offset;
                }

                offset = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
            }

            if (level.isEmptyBlock(offset) && state.canSurvive(level, offset)) {
                level.setBlock(offset, state, 2);
            }
        }
    }

    private boolean canSpread(ServerLevel level, BlockPos pos) {
        BlockPos minPos = pos.offset(-4, -1, -4);
        BlockPos maxPos = pos.offset(4, 1, 4);
        return level.findBlocksIn(minPos, maxPos).filterState(state -> state.is(this)).atMostMatched(4);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isSolidRender();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        var soilDecision = below.canSustainPlant(level, belowPos, net.minecraft.core.Direction.UP, state);
        return below.is(BlockTags.OVERRIDES_MUSHROOM_LIGHT_REQUIREMENT) ? true : soilDecision.isDefault() ? (level.getRawBrightness(pos, 0) < 13 && this.mayPlaceOn(below, level, belowPos)) : soilDecision.isTrue();
    }

    public boolean growMushroom(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        Optional<? extends Holder<Feature>> feature = level.registryAccess().lookupOrThrow(Registries.FEATURE).get(this.feature);

        // Neo: Fire the BlockGrowFeatureEvent and update the result of the Optional local with the new feature.
        var event = net.neoforged.neoforge.event.EventHooks.fireBlockGrowFeature(level, random, pos, feature.orElse(null));
        if (event.isCanceled()) {
            return false;
        }
        feature = Optional.ofNullable(event.getFeature());

        if (feature.isEmpty()) {
            return false;
        }

        level.removeBlock(pos, false);
        if (feature.get().value().place(level, level.getChunkSource().getGenerator(), random, pos)) {
            return true;
        }

        level.setBlockAndUpdate(pos, state);
        return false;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        if (level instanceof ServerLevel serverLevel) {
            Optional<? extends Holder<Feature>> featureHolder = serverLevel.registryAccess().lookupOrThrow(Registries.FEATURE).get(this.feature);
            if (featureHolder.isPresent()) {
                Feature feature = featureHolder.get().value();
                if (feature instanceof AbstractHugeMushroomFeature mushroomFeature) {
                    int minHeight = 4 + mushroomFeature.foliageRadius();
                    return level.isInsideBuildHeight(pos.above(minHeight));
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return random.nextFloat() < 0.4;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        this.growMushroom(level, pos, state, random);
    }
}
