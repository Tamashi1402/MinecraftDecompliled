package net.minecraft.client.model.monster.zombie;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GiantZombieModel extends HumanoidModel<ZombieRenderState> {
    public GiantZombieModel(ModelPart root) {
        super(root);
    }

    protected void setupAttackAnimation(ZombieRenderState state) {
        super.setupAttackAnimation(state);
        AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, state.isAggressive, state);
    }
}
