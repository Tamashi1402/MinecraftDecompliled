package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

public abstract class MultifaceSpreadeableBlock extends MultifaceBlock {
    public MultifaceSpreadeableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public abstract MultifaceSpreader getSpreader();
}
