package com.mojang.renderpearl.api.device;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GpuOutOfMemoryException extends RuntimeException {
    public GpuOutOfMemoryException(String message) {
        super(message);
    }
}
