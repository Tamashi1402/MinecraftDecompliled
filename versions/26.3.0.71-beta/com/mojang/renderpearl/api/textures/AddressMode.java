package com.mojang.renderpearl.api.textures;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@net.neoforged.neoforge.internal.NonExhaustiveEnum(reason = "Further address modes may be added")
public enum AddressMode {
    REPEAT,
    CLAMP_TO_EDGE;
}
