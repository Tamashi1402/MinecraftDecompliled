package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitchItemLayer extends CrossedArmsItemLayer<WitchRenderState, WitchModel> {
    public WitchItemLayer(RenderLayerParent<WitchRenderState, WitchModel> renderer) {
        super(renderer);
    }

    protected void applyTranslation(WitchRenderState state, PoseStack poseStack) {
        if (state.isHoldingPotion) {
            this.getParentModel().root().translateAndRotate(poseStack);
            this.getParentModel().translateToHead(poseStack);
            this.getParentModel().getNose().translateAndRotate(poseStack);
            poseStack.translate(0.0625F, 0.25F, 0.0F);
            poseStack.rotateDegrees(Axis.ZP, 180.0F);
            poseStack.rotateDegrees(Axis.XP, 140.0F);
            poseStack.rotateDegrees(Axis.ZP, 10.0F);
            poseStack.rotateDegrees(Axis.XP, 180.0F);
        } else {
            super.applyTranslation(state, poseStack);
        }
    }
}
