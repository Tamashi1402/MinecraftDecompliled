package net.minecraft.client.renderer.oit;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum OitStage {
    DEPTH_BOUNDS,
    TRANSMITTANCE,
    ACCUMULATE;
}
