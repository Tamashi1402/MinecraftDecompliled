package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderBlock extends FallingBlock {
    private final Block concrete;

    public ConcretePowderBlock(Block concrete, BlockBehaviour.Properties properties) {
        super(properties);
        this.concrete = concrete;
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState replacedBlock, FallingBlockEntity entity) {
        if (shouldSolidify(level, pos, state, replacedBlock.getFluidState())) { // Forge: Use block of falling entity instead of block at replaced position, and check if shouldSolidify with the FluidState of the replaced block
            level.setBlockAndUpdate(pos, this.concrete.defaultBlockState());
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState replacedBlock = level.getBlockState(pos);
        return shouldSolidify(level, pos, replacedBlock) ? this.concrete.defaultBlockState() : super.getStateForPlacement(context);
    }

    private static boolean shouldSolidify(BlockGetter level, BlockPos pos, BlockState replacedBlock, net.minecraft.world.level.material.FluidState fluidState) {
        return replacedBlock.canBeHydrated(level, pos, fluidState) || touchesLiquid(level, pos, replacedBlock);
    }

    private static boolean shouldSolidify(BlockGetter level, BlockPos pos, BlockState replacedBlock) {
        return shouldSolidify(level, pos, replacedBlock, level.getFluidState(pos));
    }

    private static boolean touchesLiquid(BlockGetter level, BlockPos pos, BlockState state) {
        boolean touchesLiquid = false;
        BlockPos.MutableBlockPos testPos = pos.mutable();

        for (Direction direction : Direction.values()) {
            BlockState blockState = level.getBlockState(testPos);
            if (direction != Direction.DOWN || state.canBeHydrated(level, pos, blockState.getFluidState())) {
                testPos.setWithOffset(pos, direction);
                blockState = level.getBlockState(testPos);
                if (state.canBeHydrated(level, pos, blockState.getFluidState()) && !blockState.isFaceSturdy(level, pos, direction.getOpposite())) {
                    touchesLiquid = true;
                    break;
                }
            }
        }

        return touchesLiquid;
    }

    private static boolean canSolidify(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction directionToNeighbour,
        BlockPos neighbourPos,
        BlockState neighbourState,
        RandomSource random
    ) {
        return touchesLiquid(level, pos, state)
            ? this.concrete.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public int getDustColor(BlockState blockState, BlockGetter level, BlockPos pos) {
        return blockState.getMapColor(level, pos).col;
    }
}
