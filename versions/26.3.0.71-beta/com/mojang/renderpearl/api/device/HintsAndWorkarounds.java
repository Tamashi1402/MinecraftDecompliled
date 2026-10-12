package com.mojang.renderpearl.api.device;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record HintsAndWorkarounds(
    boolean writeToBufferIsSlow, boolean anisotropyHasKnownIssues, boolean isExplicitDepthRequired, boolean multiDrawIndirectHasKnownIssues
) {
}
