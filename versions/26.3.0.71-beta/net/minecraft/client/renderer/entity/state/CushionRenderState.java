package net.minecraft.client.renderer.entity.state;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CushionRenderState extends EntityRenderState {
    private static final Identifier DEFAULT_TEXTURE = Identifier.withDefaultNamespace("textures/entity/cushion/white_cushion.png");
    public Direction direction = Direction.NORTH;
    public Identifier texture = DEFAULT_TEXTURE;
}
