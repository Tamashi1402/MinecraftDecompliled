package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.material.Fluid;

public class Swim<T extends Mob> extends Behavior<T> {
    private final float chance;
    private final TagKey<Fluid> fluid;

    public Swim(float chance) {
        this(chance, FluidTags.ENTITY_FLOATABLE);
    }

    public Swim(float chance, TagKey<Fluid> fluid) {
        super(ImmutableMap.of());
        this.chance = chance;
        this.fluid = fluid;
    }

    protected boolean checkExtraStartConditions(ServerLevel level, Mob body) {
        return body.isInFluidDeeperThan(body.getFluidJumpThreshold(), this.fluid) || body.isInLava() || body.getFluidInteraction().isInFluidMatching(body, (entity, fluidType, height) -> entity.canSwimInFluidType(fluidType) && height > entity.getFluidJumpThreshold());
    }

    protected boolean canStillUse(ServerLevel level, Mob body, long timestamp) {
        return this.checkExtraStartConditions(level, body);
    }

    protected void tick(ServerLevel level, Mob body, long timestamp) {
        if (body.getRandom().nextFloat() < this.chance) {
            body.getJumpControl().jump();
        }
    }
}
