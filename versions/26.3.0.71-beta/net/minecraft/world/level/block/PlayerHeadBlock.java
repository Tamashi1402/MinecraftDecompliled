package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class PlayerHeadBlock extends SkullBlock {
    public PlayerHeadBlock(BlockBehaviour.Properties properties) {
        super(SkullBlock.Types.PLAYER, properties);
    }
}
