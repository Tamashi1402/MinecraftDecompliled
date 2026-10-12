package net.minecraft.client.renderer.state.level;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TransientBlockRenderState {
    public MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
    public long createTimeNs;
    public long liveUntilMs;

    public record Removal(long sectionNode, long compileTaskStartTimeNs) {
    }
}
