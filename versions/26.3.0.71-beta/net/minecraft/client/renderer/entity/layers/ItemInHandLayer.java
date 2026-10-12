package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemInHandLayer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel<S>> extends RenderLayer<S, M> {
    public ItemInHandLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        this.submitArmWithItem(state, state.rightHandItemState, state.rightHandItemStack, HumanoidArm.RIGHT, poseStack, submitNodeCollector, lightCoords);
        this.submitArmWithItem(state, state.leftHandItemState, state.leftHandItemStack, HumanoidArm.LEFT, poseStack, submitNodeCollector, lightCoords);
    }

    protected void submitArmWithItem(
        S state, ItemStackRenderState item, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords
    ) {
        if (!item.isEmpty()) {
            poseStack.pushPose();
            this.getParentModel().translateToHand(state, arm, poseStack);
            poseStack.rotateDegrees(Axis.XP, -90.0F);
            poseStack.rotateDegrees(Axis.YP, 180.0F);
            boolean isLeftHand = arm == HumanoidArm.LEFT;
            float offsetX = this.useBabyOffset(state) ? 0.0F : 1.0F;
            float offsetY = this.useBabyOffset(state) ? 1.0F : 2.0F;
            float offsetZ = this.useBabyOffset(state) ? -4.5F : -10.0F;
            poseStack.translate((isLeftHand ? -1 : 1) * offsetX / 16.0F, offsetY / 16.0F, offsetZ / 16.0F);
            if (state.currentSwing != null
                && state.currentSwing.hand().asArm(state.mainArm) == arm
                && state.currentSwing.animation().type() == SwingAnimationType.STAB) {
                SpearAnimations.thirdPersonAttackItem(state, poseStack);
            }

            float ticksUsingItem = state.ticksUsingItem(arm);
            if (ticksUsingItem != 0.0F) {
                (arm == HumanoidArm.RIGHT ? state.rightArmPose : state.leftArmPose).animateUseItem(state, poseStack, ticksUsingItem, arm, itemStack);
            }

            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }

    private boolean useBabyOffset(S state) {
        return state.isBaby && state.entityType != EntityTypes.ARMOR_STAND;
    }
}
