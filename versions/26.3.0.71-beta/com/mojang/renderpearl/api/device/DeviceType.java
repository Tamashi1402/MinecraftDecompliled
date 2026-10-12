package com.mojang.renderpearl.api.device;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum DeviceType {
    OTHER,
    INTEGRATED,
    DISCRETE,
    VIRTUAL,
    CPU;
}
