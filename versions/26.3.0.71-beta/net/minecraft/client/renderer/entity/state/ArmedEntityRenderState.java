package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ArmedEntityRenderState extends LivingEntityRenderState {
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public HumanoidModel.ArmPose rightArmPose = HumanoidModel.ArmPose.EMPTY;
    public final ItemStackRenderState rightHandItemState = new ItemStackRenderState();
    public ItemStack rightHandItemStack = ItemStack.EMPTY;
    public HumanoidModel.ArmPose leftArmPose = HumanoidModel.ArmPose.EMPTY;
    public final ItemStackRenderState leftHandItemState = new ItemStackRenderState();
    public ItemStack leftHandItemStack = ItemStack.EMPTY;
    public LivingEntity.@Nullable SwingDescription currentSwing;
    public float swingAnimation;

    public ItemStackRenderState getMainHandItemState() {
        return this.mainArm == HumanoidArm.RIGHT ? this.rightHandItemState : this.leftHandItemState;
    }

    public ItemStack getMainHandItemStack() {
        return this.mainArm == HumanoidArm.RIGHT ? this.rightHandItemStack : this.leftHandItemStack;
    }

    public ItemStack getUseItemStackForArm(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? this.rightHandItemStack : this.leftHandItemStack;
    }

    public HumanoidModel.ArmPose getArmPose(HumanoidArm arm) {
        return switch (arm) {
            case LEFT -> this.leftArmPose;
            case RIGHT -> this.rightArmPose;
        };
    }

    public float ticksUsingItem(HumanoidArm arm) {
        return 0.0F;
    }

    public static void extractArmedEntityRenderState(LivingEntity entity, ArmedEntityRenderState state, ItemModelResolver itemModelResolver, float partialTicks) {
        state.mainArm = entity.getMainArm();
        state.currentSwing = entity.getCurrentSwing();
        state.swingAnimation = entity.getSwingAnimation(partialTicks);
        itemModelResolver.updateForLiving(
            state.rightHandItemState, entity.getItemHeldByArm(HumanoidArm.RIGHT), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity
        );
        itemModelResolver.updateForLiving(state.leftHandItemState, entity.getItemHeldByArm(HumanoidArm.LEFT), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
        state.leftHandItemStack = entity.getItemHeldByArm(HumanoidArm.LEFT).copy();
        state.rightHandItemStack = entity.getItemHeldByArm(HumanoidArm.RIGHT).copy();
    }
}
