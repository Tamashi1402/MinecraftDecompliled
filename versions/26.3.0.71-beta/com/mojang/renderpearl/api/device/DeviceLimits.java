package com.mojang.renderpearl.api.device;

import com.mojang.renderpearl.api.GpuFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DeviceLimits(
    int maxAnisotropy,
    int minUniformOffsetAlignment,
    int maxTextureSize,
    long maxMemoryAllocationSize,
    int maxMultiDrawDirectInterleavedDrawCount,
    int maxColorAttachments,
    int maxDrawIndirectDrawCount
) {
    public int maxTextureSizeForFormat(GpuFormat format) {
        return Integer.highestOneBit(Math.min(this.maxTextureSize, (int)Math.sqrt((double)this.maxMemoryAllocationSize / format.blockSize())));
    }
}
