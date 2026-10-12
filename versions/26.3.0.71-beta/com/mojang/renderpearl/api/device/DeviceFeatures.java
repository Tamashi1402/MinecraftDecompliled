package com.mojang.renderpearl.api.device;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DeviceFeatures(
    boolean wireframeFillMode,
    boolean shaderDrawParameters,
    boolean multiDrawDirectInterleaved,
    boolean multiDrawDirectSeparate,
    boolean multiDrawIndirect,
    boolean drawIndirect,
    boolean nonZeroFirstInstance,
    boolean persistentMapping
) {
}
