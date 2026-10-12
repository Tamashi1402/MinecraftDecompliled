package com.mojang.renderpearl.backend.vulkan;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface Destroyable {
    void destroy();
}
