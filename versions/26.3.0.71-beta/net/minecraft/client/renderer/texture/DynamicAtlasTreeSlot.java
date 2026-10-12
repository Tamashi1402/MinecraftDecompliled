package net.minecraft.client.renderer.texture;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface DynamicAtlasTreeSlot {
    int x();

    int y();

    int width();

    int height();
}
