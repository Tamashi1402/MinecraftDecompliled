package com.mojang.renderpearl.api.pipeline;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BlendEquation(BlendFactor sourceFactor, BlendFactor destFactor, BlendOp op) {
}
