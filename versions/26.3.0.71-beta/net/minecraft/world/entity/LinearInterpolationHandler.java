package net.minecraft.world.entity;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class LinearInterpolationHandler extends AbstractInterpolationHandler {
    public static final int DEFAULT_INTERPOLATION_STEPS = 3;
    private final LinearInterpolationHandler.InterpolationData interpolationData = new LinearInterpolationHandler.InterpolationData();

    private LinearInterpolationHandler(Entity entity, int interpolationSteps) {
        super(entity, interpolationSteps);
    }

    public static InterpolationHandler create(Entity entity, int interpolationSteps) {
        return entity.level().isClientSide() ? new LinearInterpolationHandler(entity, interpolationSteps) : InterpolationHandler.NO_OP;
    }

    public static InterpolationHandler create(Entity entity) {
        return create(entity, 3);
    }

    public void setInterpolationLength(int steps) {
        this.interpolationSteps = steps;
    }

    @Override
    protected PositionAndRotation.Mutable interpolationData() {
        return this.interpolationData;
    }

    @Override
    protected void startInterpolating(PositionPath position, float yRot, float xRot) {
        this.interpolationData.set(position.endPosition(), yRot, xRot);
        this.interpolationData.remainingSteps = this.interpolationSteps;
    }

    @Override
    protected void doInterpolate() {
        double alpha = 1.0 / this.interpolationData.remainingSteps;
        Vec3 newPosition = this.entity.position().lerp(this.interpolationData.position(), alpha);
        float newYRot = (float)Mth.rotLerp(alpha, this.entity.getYRot(), this.interpolationData.yRot());
        float newXRot = (float)Mth.lerp(alpha, this.entity.getXRot(), this.interpolationData.xRot());
        this.entity.setPos(newPosition);
        this.entity.setRot(newYRot, newXRot);
        this.interpolationData.remainingSteps--;
    }

    @Override
    public boolean hasActiveInterpolation() {
        return this.interpolationData.remainingSteps > 0;
    }

    @Override
    public void cancel() {
        this.interpolationData.remainingSteps = 0;
    }

    private static class InterpolationData extends PositionAndRotation.Mutable {
        private int remainingSteps;
    }
}
