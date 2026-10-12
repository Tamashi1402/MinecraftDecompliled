package com.mojang.renderpearl.api.textures;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum FilterMode {
    NEAREST,
    LINEAR;
}
