package net.minecraft.world.entity;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
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
    private final Map<net.neoforged.neoforge.fluids.FluidType, EntityFluidInteraction.Tracker> trackerByFluid = new Reference2ObjectArrayMap<>();

    public EntityFluidInteraction(Set<TagKey<Fluid>> fluids) {
        for (TagKey<Fluid> fluid : fluids) {
            // Neo: Handle tracked fluids more generically
            //this.trackerByFluid.put(fluid, new EntityFluidInteraction.Tracker());
        }
    }

    public void update(Entity entity, boolean ignoreCurrent) {
        this.trackerByFluid.values().removeIf(EntityFluidInteraction.Tracker::resetAndCheckUnused);
        AABB box = entity.getFluidInteractionBox();
        if (box != null) {
            int x0 = Mth.floor(box.minX);
            int y0 = Mth.floor(box.minY);
            int z0 = Mth.floor(box.minZ);
            int x1 = Mth.ceil(box.maxX) - 1;
            int y1 = Mth.ceil(box.maxY) - 1;
            int z1 = Mth.ceil(box.maxZ) - 1;
            if (hasFluidAndLoaded(entity.level(), x0 - 1, y0, z0 - 1, x1 + 1, y1, z1 + 1)) {
                double entityY = entity.getBoundingBox().minY;
                int eyeBlockX = entity.getBlockX();
                double eyeY = entity.getEyeY();
                int eyeBlockZ = entity.getBlockZ();
                net.neoforged.neoforge.fluids.FluidType lastFluidType = null;
                EntityFluidInteraction.Tracker tracker = null;
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
                                    var fluidType = fluidState.getType().getFluidType();
                                    if (fluidType != lastFluidType) {
                                        lastFluidType = fluidType;
                                        tracker = this.getTrackerFor(fluidType);
                                    }

                                    if (tracker != null) {
                                        tracker.idleTicks = 0; // Neo: reset idle counter

                                        if (x == eyeBlockX && z == eyeBlockZ && eyeY >= fluidBottom && eyeY <= fluidTop) {
                                            tracker.eyesInside = true;
                                        }

                                        tracker.height = Math.max(fluidTop - entityY, tracker.height);
                                        if (!ignoreCurrent) {
                                            Vec3 flow = fluidState.getFlow(level, mutablePos);
                                            if (tracker.height < 0.4) {
                                                flow = flow.scale(tracker.height);
                                            }

                                            tracker.accumulateCurrent(flow);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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

    /// @deprecated Neo: use [#getTrackerFor(net.neoforged.neoforge.fluids.FluidType)] instead
    @Deprecated
    private EntityFluidInteraction.@Nullable Tracker getTrackerFor(Fluid fluid) {
        return this.getTrackerFor(fluid.getFluidType());
    }

    private EntityFluidInteraction.Tracker getTrackerFor(net.neoforged.neoforge.fluids.FluidType fluid) {
        return this.trackerByFluid.computeIfAbsent(fluid, _ -> new Tracker());
    }

    /// @deprecated Neo: use [#applyCurrentTo(net.neoforged.neoforge.fluids.FluidType, Entity, double)] instead
    @Deprecated
    public void applyCurrentTo(TagKey<Fluid> fluid, Entity entity, double scale) {
        this.applyCurrentTo(getFluidTypeByTag(fluid), entity, scale);
    }

    public void applyCurrentTo(net.neoforged.neoforge.fluids.FluidType fluid, Entity entity, double scale) {
        EntityFluidInteraction.Tracker tracker = this.trackerByFluid.get(fluid);
        if (tracker != null) {
            tracker.applyCurrentTo(entity, scale);
        }
    }

    public void applyCurrentTo(Entity entity) {
        for (var entry : this.trackerByFluid.entrySet()) {
            if (entry.getValue().height > 0D) {
                entry.getValue().applyCurrentTo(entity, entity.getFluidMotionScale(entry.getKey()));
            }
        }
    }

    /// @deprecated Neo: use [#getFluidHeight(net.neoforged.neoforge.fluids.FluidType)] instead
    @Deprecated
    public double getFluidHeight(TagKey<Fluid> fluid) {
        return this.getFluidHeight(getFluidTypeByTag(fluid));
    }

    public double getFluidHeight(net.neoforged.neoforge.fluids.FluidType fluid) {
        EntityFluidInteraction.Tracker tracker = this.trackerByFluid.get(fluid);
        return tracker != null ? tracker.height : 0.0;
    }

    public <E extends Entity> double getMaxFluidHeightMatching(E entity, net.neoforged.neoforge.fluids.InFluidPredicate<E> predicate) {
        double maxHeight = 0D;
        for (var entry : this.trackerByFluid.entrySet()) {
            double height = entry.getValue().height;
            if (height > maxHeight && predicate.test(entity, entry.getKey(), height)) {
                maxHeight = height;
            }
        }
        return maxHeight;
    }

    public net.neoforged.neoforge.fluids.FluidType getMaxHeightFluidType() {
        double maxHeight = 0D;
        var maxHeightType = net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE.value();
        for (var entry : this.trackerByFluid.entrySet()) {
            double height = entry.getValue().height;
            if (height > maxHeight) {
                maxHeight = height;
                maxHeightType = entry.getKey();
            }
        }
        return maxHeightType;
    }

    /// @deprecated Neo: use [#isInFluid(net.neoforged.neoforge.fluids.FluidType)] instead
    @Deprecated
    public boolean isInFluid(TagKey<Fluid> fluid) {
        return this.isInFluid(getFluidTypeByTag(fluid));
    }

    public boolean isInFluid(net.neoforged.neoforge.fluids.FluidType fluid) {
        return this.getFluidHeight(fluid) > 0.0;
    }

    public boolean isInAnyFluid() {
        for (Tracker tracker : this.trackerByFluid.values()) {
            if (tracker.height > 0D) {
                return true;
            }
        }
        return false;
    }

    public <E extends Entity> boolean isInFluidMatching(E entity, net.neoforged.neoforge.fluids.InFluidPredicate<E> predicate) {
        for (var entry : this.trackerByFluid.entrySet()) {
            double height = entry.getValue().height;
            if (height > 0D && predicate.test(entity, entry.getKey(), height)) {
                return true;
            }
        }
        return false;
    }

    /// @deprecated Neo: use [#isEyeInFluid(net.neoforged.neoforge.fluids.FluidType)] instead
    @Deprecated
    public boolean isEyeInFluid(TagKey<Fluid> fluid) {
        return this.isEyeInFluid(getFluidTypeByTag(fluid));
    }

    public boolean isEyeInFluid(net.neoforged.neoforge.fluids.FluidType fluid) {
        EntityFluidInteraction.Tracker tracker = this.trackerByFluid.get(fluid);
        return tracker != null && tracker.eyesInside;
    }

    public <E extends Entity> boolean isEyeInFluidMatching(E entity, net.neoforged.neoforge.fluids.InFluidPredicate<E> predicate) {
        for (var entry : this.trackerByFluid.entrySet()) {
            Tracker tracker = entry.getValue();
            if (tracker.eyesInside && predicate.test(entity, entry.getKey(), tracker.height)) {
                return true;
            }
        }
        return false;
    }

    public net.neoforged.neoforge.fluids.FluidType getFirstEyeInFluid() {
        for (var entry : this.trackerByFluid.entrySet()) {
            if (entry.getValue().eyesInside) {
                return entry.getKey();
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

    private static class Tracker {
        private double height;
        private boolean eyesInside;
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
            this.eyesInside = false;
            this.accumulatedCurrent = Vec3.ZERO;
            this.currentCount = 0;
        }

        public void accumulateCurrent(Vec3 flow) {
            this.accumulatedCurrent = this.accumulatedCurrent.add(flow);
            this.currentCount++;
        }

        public void applyCurrentTo(Entity entity, double scale) {
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
}
