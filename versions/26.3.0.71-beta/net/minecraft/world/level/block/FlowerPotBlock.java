package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlowerPotBlock extends Block {
    private static final Map<Block, Block> POTTED_BY_CONTENT = Maps.newHashMap();
    private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 6.0);
    /// Neo: Field accesses are redirected to {@link #getPotted()} with a coremod.
    private final Block potted;
    private final java.util.function.@org.jspecify.annotations.Nullable Supplier<FlowerPotBlock> emptyPot;
    private final java.util.function.Supplier<? extends Block> flowerDelegate;

    /// @deprecated Neo: Use the other constructors for [empty pots][#FlowerPotBlock(Properties)] and [full pots][#FlowerPotBlock(java.util.function.Supplier, java.util.function.Supplier, Properties)].
    @Deprecated
    public FlowerPotBlock(Block potted, BlockBehaviour.Properties properties) {
        super(properties);
        this.potted = null; // Unused, redirected by coremod
        this.flowerDelegate = () -> potted;
        // If we're constructing the empty pot at Blocks.FLOWER_POT, then the field will be null
        this.emptyPot = Blocks.FLOWER_POT == null ? null : () -> (FlowerPotBlock) Blocks.FLOWER_POT;
    }

    /// Neo: Constructs a flower pot with the given empty pot and potted flower/plant.
    /// For mod use, eliminates the need to extend this class, and prevents modded flower pots from altering vanilla behavior.
    ///
    /// Use the [other constructor][#FlowerPotBlock(Properties)] for constructing empty pots.
    ///
    /// @param emptyPot The empty pot for this pot
    /// @param potted The potted flower/plant
    public FlowerPotBlock(java.util.function.Supplier<FlowerPotBlock> emptyPot, java.util.function.Supplier<? extends Block> potted, BlockBehaviour.Properties properties) {
        super(properties);
        this.potted = null; // Unused, redirected by coremod
        this.flowerDelegate = java.util.Objects.requireNonNull(potted, "Use single-parameter constructor instead for empty pots");
        this.emptyPot = java.util.Objects.requireNonNull(emptyPot, "Use single-parameter constructor instead for empty pots");
    }

    /// Neo: Constructs an empty flower pot.
    public FlowerPotBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.potted = null; // Unused, redirected by coremod
        this.flowerDelegate = () -> Blocks.AIR;
        this.emptyPot = null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        BlockState newContents = (itemStack.getItem() instanceof BlockItem blockItem
                ? emptyPot == null ? getFullPot(blockItem.getBlock()) : Blocks.AIR
                : Blocks.AIR)
            .defaultBlockState();
        if (newContents.isAir()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!this.isEmpty()) {
            return InteractionResult.CONSUME;
        }

        level.setBlockAndUpdate(pos, newContents);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        player.awardStat(Stats.POT_FLOWER);
        itemStack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (this.isEmpty()) {
            return InteractionResult.CONSUME;
        }

        ItemStack plant = new ItemStack(this.potted);
        if (!player.addItem(plant)) {
            player.drop(plant, false, Prediction.PREDICTED);
        }

        level.setBlockAndUpdate(pos, getEmptyPot().defaultBlockState());
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return this.isEmpty() ? super.getCloneItemStack(level, pos, state, includeData) : new ItemStack(this.potted);
    }

    private boolean isEmpty() {
        return this.potted == Blocks.AIR;
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
        return directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    public Block getPotted() {
        return flowerDelegate.get();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    public FlowerPotBlock getEmptyPot() {
         return emptyPot == null ? this : emptyPot.get();
    }

    /// Neo: {@return an unmodifiable view of the map of plant blocks to their associated full pot block}
    ///
    /// @throws IllegalStateException if this is called on a non-empty pot
    public Map<Block, Block> getFullPots() {
        if (emptyPot != null) {
            throw new IllegalStateException("Full pots can only be queried from the empty pot");
        }
        return java.util.Collections.unmodifiableMap(net.neoforged.neoforge.registries.GameData.getFlowerPotBlockTable().row(this));
    }

    /// Neo: {@return the full pot associated with the given plant block, or [Blocks#AIR] if there is no associated full pot}
    ///
    /// @param plant the plant block
    /// @throws IllegalStateException if this is called on a non-empty pot
    public Block getFullPot(Block plant) {
        if (emptyPot != null) {
            throw new IllegalStateException("Full pots can only be queried from the empty pot");
        }
        return net.neoforged.neoforge.registries.GameData.getFlowerPotBlockTable().row(this).getOrDefault(plant, Blocks.AIR);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.is(Blocks.POTTED_OPEN_EYEBLOSSOM) || state.is(Blocks.POTTED_CLOSED_EYEBLOSSOM);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.isRandomlyTicking(state)) {
            boolean isOpen = this.potted == Blocks.OPEN_EYEBLOSSOM;
            boolean shouldBeOpen = level.environmentAttributes().getValue(EnvironmentAttributes.EYEBLOSSOM_OPEN, pos).toBoolean(isOpen);
            if (isOpen != shouldBeOpen) {
                level.setBlockAndUpdate(pos, this.opposite(state));
                EyeblossomBlock.Type newType = EyeblossomBlock.Type.fromBoolean(isOpen).transform();
                newType.spawnTransformParticle(level, pos, random);
                level.playSound(null, pos, newType.longSwitchSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

        super.randomTick(state, level, pos, random);
    }

    public BlockState opposite(BlockState state) {
        if (state.is(Blocks.POTTED_OPEN_EYEBLOSSOM)) {
            return Blocks.POTTED_CLOSED_EYEBLOSSOM.defaultBlockState();
        } else {
            return state.is(Blocks.POTTED_CLOSED_EYEBLOSSOM) ? Blocks.POTTED_OPEN_EYEBLOSSOM.defaultBlockState() : state;
        }
    }
}
