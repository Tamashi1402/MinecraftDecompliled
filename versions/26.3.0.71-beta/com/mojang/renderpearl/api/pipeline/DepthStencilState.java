package com.mojang.renderpearl.api.pipeline;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DepthStencilState(CompareOp depthTest, boolean writeDepth, float depthBiasScaleFactor, float depthBiasConstant, net.neoforged.neoforge.client.stencil.@org.jspecify.annotations.Nullable StencilTest stencilTest) {
    public static final DepthStencilState DEFAULT = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true);

    public DepthStencilState(CompareOp depthTest, boolean writeDepth, float depthBiasScaleFactor, float depthBiasConstant) {
        this(depthTest, writeDepth, depthBiasScaleFactor, depthBiasConstant, null);
    }

    public DepthStencilState(CompareOp depthTest, boolean depthWrite) {
        this(depthTest, depthWrite, 0.0F, 0.0F);
    }
}
