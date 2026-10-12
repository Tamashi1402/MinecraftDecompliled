package net.minecraft.world.level.block.state;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

import net.neoforged.neoforge.common.extensions.IBlockStateExtension;

public class BlockState extends BlockBehaviour.BlockStateBase implements IBlockStateExtension {
    public static final Codec<BlockState> FULL_CODEC = codec(BuiltInRegistries.BLOCK.byNameCodec(), Block::defaultBlockState, Block::getStateDefinition)
        .stable();
    private static final Codec<Either<Block, BlockState>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(BuiltInRegistries.BLOCK.byNameCodec(), FULL_CODEC);
    public static final Codec<BlockState> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap(
        either -> either.map(Block::defaultBlockState, f -> (BlockState)f),
        state -> state == state.getBlock().defaultBlockState() ? Either.left(state.getBlock()) : Either.right(state)
    );

    public BlockState(Block owner, Property<?>[] propertyKeys, Comparable<?>[] propertyValues) {
        super(owner, propertyKeys, propertyValues);
    }

    @Override
    protected BlockState asState() {
        return this;
    }
}
