package net.minecraft.client.renderer.extract;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record TransientBlock(BlockPos pos, BlockState state, double timeToLive, long createTimeNs) {
    public static double DEFAULT_TIME_TO_LIVE = 1.0;
}
