package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface UvMapping {
    float getU(float offset);

    float getV(float offset);

    default VertexConsumer wrap(VertexConsumer buffer) {
        return new SpriteCoordinateExpander(buffer, this);
    }
}
