package net.minecraft.world.entity;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityFluidInteraction {
    private final List<EntityFluidInteraction.Tracker> fluidTrackers = new ArrayList<>();
    private final Reference2ObjectMap<net.neoforged.neoforge.fluids.FluidType, EntityFluidInteraction.CurrentAccumulator> currentAccumulators = new Reference2ObjectArrayMap<>();

    public EntityFluidInteraction(Set<TagKey<Fluid>> fluidsWithCurrent) {
        for (TagKey<Fluid> fluid : fluidsWithCurrent) {
            // Neo: Handle tracked fluids more generically
            //this.currentAccumulators.put(fluid, new EntityFluidInteraction.CurrentAccumulator());
        }
    }

    public boolean update(Entity entity, boolean ignoreCurrent) {
        return this.update(entity, _ -> !ignoreCurrent);
    }

    public boolean update(Entity entity, java.util.function.Predicate<net.neoforged.neoforge.fluids.FluidType> typePushPredicate) {
        this.fluidTrackers.removeIf(EntityFluidInteraction.Tracker::reset);
        this.currentAccumulators.values().removeIf(EntityFluidInteraction.CurrentAccumulator::resetAndCheckUnused);
        AABB box = entity.getFluidInteractionBox();
        if (box == null) {
            return false;
        }

        int x0 = Mth.floor(box.minX);
        int y0 = Mth.floor(box.minY);
        int z0 = Mth.floor(box.minZ);
        int x1 = Mth.ceil(box.maxX) - 1;
        int y1 = Mth.ceil(box.maxY) - 1;
        int z1 = Mth.ceil(box.maxZ) - 1;
        if (!hasFluidAndLoaded(entity.level(), x0 - 1, y0, z0 - 1, x1 + 1, y1, z1 + 1)) {
            return false;
        }

        double entityY = entity.getBoundingBox().minY;
        int eyeBlockX = entity.getBlockX();
        double eyeY = entity.getEyeY();
        int eyeBlockZ = entity.getBlockZ();
        Holder<Fluid> lastFluidType = null;
        EntityFluidInteraction.Tracker tracker = null;
        EntityFluidInteraction.CurrentAccumulator current = null;
        BlockGetter level = entity.level();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    mutablePos.set(x, y, z);
                    FluidState fluidState = level.getFluidState(mutablePos);
                    if (!fluidState.isEmpty()) {
                        double fluidBottom = mutablePos.getY();
                        double fluidTop = fluidBottom + fluidState.getHeight(level, mutablePos);
                        if (!(fluidTop < box.minY)) {
                            Holder<Fluid> fluidType = fluidState.typeHolder();
                            if (fluidType != lastFluidType) {
                                lastFluidType = fluidType;
                                tracker = this.getOrCreateTrackerFor(fluidType);
                                if (typePushPredicate.test(lastFluidType.value().getFluidType())) {
                                    current = this.getCurrentAccumulatorFor(fluidType);
                                } else {
                                    current = null;
                                }
                            }

                            if (x == eyeBlockX && z == eyeBlockZ && eyeY >= fluidBottom) {
                                double fluidTopForCamera = fluidBottom + fluidState.getHeightForCamera(level, mutablePos);
                                if (eyeY <= fluidTopForCamera) {
                                    tracker.eyesInside = true;
                                }
                            }

                            tracker.height = Math.max(fluidTop - entityY, tracker.height);
                            if (current != null) {
                                current.idleTicks = 0; // Neo: reset idle counter
                                Vec3 flow = fluidState.getFlow(level, mutablePos);
                                current.height = Math.max(tracker.height, current.height);
                                if (current.height < 0.4) {
                                    flow = flow.scale(current.height);
                                }

                                current.accumulate(flow);
                            }
                        }
                    }
                }
            }
        }

        return lastFluidType != null;
    }

    private static boolean hasFluidAndLoaded(Level level, int x0, int y0, int z0, int x1, int y1, int z1) {
        int sectionX0 = SectionPos.blockToSectionCoord(x0);
        int sectionY0 = SectionPos.blockToSectionCoord(y0);
        int sectionZ0 = SectionPos.blockToSectionCoord(z0);
        int sectionX1 = SectionPos.blockToSectionCoord(x1);
        int sectionY1 = SectionPos.blockToSectionCoord(y1);
        int sectionZ1 = SectionPos.blockToSectionCoord(z1);
        boolean hasFluid = false;

        for (int chunkZ = sectionZ0; chunkZ <= sectionZ1; chunkZ++) {
            for (int chunkX = sectionX0; chunkX <= sectionX1; chunkX++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    return false;
                }

                LevelChunkSection[] sections = chunk.getSections();

                for (int sectionY = sectionY0; sectionY <= sectionY1; sectionY++) {
                    int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
                    if (sectionIndex >= 0 && sectionIndex < sections.length) {
                        hasFluid |= sections[sectionIndex].hasFluid();
                    }
                }
            }
        }

        return hasFluid;
    }

    private EntityFluidInteraction.Tracker getOrCreateTrackerFor(Holder<Fluid> fluidType) {
        for (EntityFluidInteraction.Tracker tracker : this.fluidTrackers) {
            if (tracker.fluidType.equals(fluidType)) {
                return tracker;
            }
        }

        EntityFluidInteraction.Tracker tracker = new EntityFluidInteraction.Tracker(fluidType);
        this.fluidTrackers.add(tracker);
        return tracker;
    }

    /// @deprecated Neo: use [#getCurrentAccumulatorFor(net.neoforged.neoforge.fluids.FluidType)] instead
    @Deprecated
    private EntityFluidInteraction.@Nullable CurrentAccumulator getCurrentAccumulatorFor(Holder<Fluid> fluid) {
        return this.getCurrentAccumulatorFor(fluid.value().getFluidType());
    }

    private EntityFluidInteraction.CurrentAccumulator getCurrentAccumulatorFor(net.neoforged.neoforge.fluids.FluidType fluid) {
        return this.currentAccumulators.computeIfAbsent(fluid, _ -> new CurrentAccumulator());
    }

    /// @deprecated Neo: use [#applyCurrentTo(net.neoforged.neoforge.fluids.FluidType, Entity, double)] instead
    @Deprecated
    public void applyCurrentTo(TagKey<Fluid> fluid, Entity entity, double scale) {
        this.applyCurrentTo(getFluidTypeByTag(fluid), entity, scale);
    }

    public void applyCurrentTo(net.neoforged.neoforge.fluids.FluidType fluid, Entity entity, double scale) {
        EntityFluidInteraction.CurrentAccumulator current = this.currentAccumulators.get(fluid);
        if (current != null && entity.isPushedByFluid(fluid)) {
            current.applyTo(entity, scale);
        }
    }

    public void applyCurrentTo(Entity entity) {
        for (var entry : this.currentAccumulators.entrySet()) {
            if (entry.getValue().height > 0D && entity.isPushedByFluid(entry.getKey())) {
                entry.getValue().applyTo(entity, entity.getFluidMotionScale(entry.getKey()));
            }
        }
    }

    /// @deprecated Neo: use [#getFluidHeight(net.neoforged.neoforge.fluids.FluidType)] instead
    @Deprecated
    public double getFluidHeight(TagKey<Fluid> fluid) {
        double height = 0.0;

        for (EntityFluidInteraction.Tracker tracker : this.fluidTrackers) {
            if (tracker.height > height && tracker.fluidType.is(fluid)) {
                height = tracker.height;
            }
        }

        return height;
    }

    public double getFluidHeight(net.neoforged.neoforge.fluids.FluidType fluid) {
        double height = 0.0;

        for (EntityFluidInteraction.Tracker tracker : this.fluidTrackers) {
            if (tracker.height > height && tracker.fluidType.value().getFluidType() == fluid) {
                height = tracker.height;
            }
        }

        return height;
    }

    public <E extends Entity> double getMaxFluidHeightMatching(E entity, net.neoforged.neoforge.fluids.InFluidPredicate<E> predicate) {
        double maxHeight = 0D;
        for (Tracker tracker : this.fluidTrackers) {
            if (tracker.height > maxHeight && predicate.test(entity, tracker.fluidType.value().getFluidType(), tracker.height)) {
                maxHeight = tracker.height;
            }
        }
        return maxHeight;
    }

    public net.neoforged.neoforge.fluids.FluidType getMaxHeightFluidType() {
        double maxHeight = 0D;
        var maxHeightType = net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE.value();
        for (Tracker tracker : this.fluidTrackers) {
            if (tracker.height > maxHeight) {
                maxHeight = tracker.height;
                maxHeightType = tracker.fluidType.value().getFluidType();
            }
        }
        return maxHeightType;
    }

    public float getFallDistanceModifier(Entity entity) {
        float minMod = 1F;
        for (Tracker tracker : this.fluidTrackers) {
            var fluidType = tracker.fluidType.value().getFluidType();
            if (tracker.height <= 0D || fluidType.isVanilla() || fluidType.getIsWaterLike()) {
                continue;
            }
            float mod = entity.getFluidFallDistanceModifier(fluidType);
            if (mod < minMod) {
                minMod = mod;
            }
        }
        return minMod;
    }

    /// @deprecated Neo: use [#isInFluid(net.neoforged.neoforge.fluids.FluidType)] instead
    @Deprecated
    public boolean isInFluid(TagKey<Fluid> fluid) {
        return this.getFluidHeight(fluid) > 0.0;
    }

    public boolean isInFluid(net.neoforged.neoforge.fluids.FluidType fluid) {
        return this.getFluidHeight(fluid) > 0.0;
    }

    public boolean isInAnyFluid() {
        for (Tracker tracker : this.fluidTrackers) {
            if (tracker.height > 0D) {
                return true;
            }
        }
        return false;
    }

    public <E extends Entity> boolean isInFluidMatching(E entity, net.neoforged.neoforge.fluids.InFluidPredicate<E> predicate) {
        for (Tracker tracker : this.fluidTrackers) {
            if (tracker.height > 0D && predicate.test(entity, tracker.fluidType.value().getFluidType(), tracker.height)) {
                return true;
            }
        }
        return false;
    }

    /// @deprecated Neo: use [#isEyeInFluid(net.neoforged.neoforge.fluids.FluidType)] instead
    @Deprecated
    public boolean isEyeInFluid(TagKey<Fluid> fluid) {
        for (EntityFluidInteraction.Tracker tracker : this.fluidTrackers) {
            if (tracker.eyesInside && tracker.fluidType.is(fluid)) {
                return true;
            }
        }

        return false;
    }

    public boolean isEyeInFluid(net.neoforged.neoforge.fluids.FluidType fluid) {
        for (EntityFluidInteraction.Tracker tracker : this.fluidTrackers) {
            if (tracker.eyesInside && tracker.fluidType.value().getFluidType() == fluid) {
                return true;
            }
        }

        return false;
    }

    public <E extends Entity> boolean isEyeInFluidMatching(E entity, net.neoforged.neoforge.fluids.InFluidPredicate<E> predicate) {
        for (Tracker tracker : this.fluidTrackers) {
            if (tracker.eyesInside && predicate.test(entity, tracker.fluidType.value().getFluidType(), tracker.height)) {
                return true;
            }
        }
        return false;
    }

    public net.neoforged.neoforge.fluids.FluidType getFirstEyeInFluid() {
        for (Tracker tracker : this.fluidTrackers) {
            if (tracker.eyesInside) {
                return tracker.fluidType.value().getFluidType();
            }
        }
        return net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE.value();
    }

    static net.neoforged.neoforge.fluids.FluidType getFluidTypeByTag(TagKey<Fluid> fluidTag) {
        if (fluidTag == net.minecraft.tags.FluidTags.WATER) {
            return net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value();
        } else if (fluidTag == net.minecraft.tags.FluidTags.LAVA) {
            return net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value();
        } else {
            throw new IllegalArgumentException("Cannot look up tracker by tag for non-vanilla fluid: " + fluidTag);
        }
    }

    private static class CurrentAccumulator {
        private double height;
        private Vec3 accumulatedCurrent = Vec3.ZERO;
        private int currentCount;
        // Neo: used to GC unused trackers
        private int idleTicks = 0;

        public boolean resetAndCheckUnused() {
            this.reset();
            this.idleTicks++;
            return this.idleTicks > 20;
        }

        public void reset() {
            this.height = 0.0;
            this.accumulatedCurrent = Vec3.ZERO;
            this.currentCount = 0;
        }

        public void accumulate(Vec3 flow) {
            this.accumulatedCurrent = this.accumulatedCurrent.add(flow);
            this.currentCount++;
        }

        public void applyTo(Entity entity, double scale) {
            if (this.currentCount != 0 && !(this.accumulatedCurrent.lengthSqr() < 1.0E-5F)) {
                Vec3 impulse;
                if (!(entity instanceof Player)) {
                    impulse = this.accumulatedCurrent.normalize();
                } else {
                    impulse = this.accumulatedCurrent.scale(1.0 / this.currentCount);
                }

                Vec3 oldMovement = entity.getDeltaMovement();
                impulse = impulse.scale(scale);
                double min = 0.003;
                if (Math.abs(oldMovement.x) < 0.003 && Math.abs(oldMovement.z) < 0.003 && impulse.length() < 0.0045000000000000005) {
                    impulse = impulse.normalize().scale(0.0045000000000000005);
                }

                entity.addDeltaMovement(impulse);
            }
        }
    }

    private static class Tracker {
        private final Holder<Fluid> fluidType;
        private double height;
        private boolean eyesInside;

        public Tracker(Holder<Fluid> fluidType) {
            this.fluidType = fluidType;
        }

        public boolean reset() {
            if (this.height == 0.0 && !this.eyesInside) {
                return true;
            }

            this.height = 0.0;
            this.eyesInside = false;
            return false;
        }
    }
}
