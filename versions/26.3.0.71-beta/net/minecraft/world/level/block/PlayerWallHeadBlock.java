package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class PlayerWallHeadBlock extends WallSkullBlock {
    public PlayerWallHeadBlock(BlockBehaviour.Properties properties) {
        super(SkullBlock.Types.PLAYER, properties);
    }
}
