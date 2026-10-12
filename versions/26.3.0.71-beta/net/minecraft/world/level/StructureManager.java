package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jspecify.annotations.Nullable;

public class StructureManager {
    private final LevelAccessor level;
    private final WorldOptions worldOptions;
    private final StructureCheck structureCheck;

    public StructureManager(LevelAccessor level, WorldOptions worldOptions, StructureCheck structureCheck) {
        this.level = level;
        this.worldOptions = worldOptions;
        this.structureCheck = structureCheck;
    }

    public StructureManager forWorldGenRegion(WorldGenRegion region) {
        if (region.getLevel() != this.level) {
            throw new IllegalStateException("Using invalid structure manager (source level: " + region.getLevel() + ", region: " + region);
        } else {
            return new StructureManager(region, this.worldOptions, this.structureCheck);
        }
    }

    public List<StructureStart> startsForStructure(int sectionX, int sectionZ, Predicate<Structure> matcher) {
        Map<Structure, LongSet> allReferences = this.level.getChunk(sectionX, sectionZ, ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
        Builder<StructureStart> result = ImmutableList.builder();

        for (Entry<Structure, LongSet> entry : allReferences.entrySet()) {
            Structure structure = entry.getKey();
            if (matcher.test(structure)) {
                this.fillStartsForStructure(structure, entry.getValue(), result::add);
            }
        }

        return result.build();
    }

    public List<StructureStart> startsForStructure(int sectionX, int sectionZ, Structure structure) {
        LongSet referencesForStructure = this.level.getChunk(sectionX, sectionZ, ChunkStatus.STRUCTURE_REFERENCES).getReferencesForStructure(structure);
        Builder<StructureStart> result = ImmutableList.builder();
        this.fillStartsForStructure(structure, referencesForStructure, result::add);
        return result.build();
    }

    public void fillStartsForStructure(Structure structure, LongSet referencesForStructure, Consumer<StructureStart> consumer) {
        for (long key : referencesForStructure) {
            ChunkPos chunkPos = ChunkPos.unpack(key);
            StructureStart start = this.getStartForStructure(structure, this.level.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.STRUCTURE_STARTS));
            if (start != null && start.isValid()) {
                consumer.accept(start);
            }
        }
    }

    public @Nullable StructureStart getStartForStructure(Structure structure, StructureAccess chunk) {
        return chunk.getStartForStructure(structure);
    }

    public void setStartForStructure(Structure structure, StructureStart start, StructureAccess chunk) {
        chunk.setStartForStructure(structure, start);
    }

    public void addReferenceForStructure(Structure structure, long reference, StructureAccess chunk) {
        chunk.addReferenceForStructure(structure, reference);
    }

    public boolean shouldGenerateStructures() {
        return this.worldOptions.generateStructures();
    }

    public StructureStart getStructureAt(BlockPos blockPos, Structure structure) {
        int sectionX = SectionPos.blockToSectionCoord(blockPos.getX());
        int sectionZ = SectionPos.blockToSectionCoord(blockPos.getZ());

        for (StructureStart structureStart : this.startsForStructure(sectionX, sectionZ, structure)) {
            if (structureStart.getBoundingBox().isInside(blockPos)) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public StructureStart getStructureAt(BlockPos blockPos, HolderSet<Structure> structures) {
        int sectionX = SectionPos.blockToSectionCoord(blockPos.getX());
        int sectionZ = SectionPos.blockToSectionCoord(blockPos.getZ());

        for (StructureStart structureStart : this.startsForStructure(sectionX, sectionZ, this.matching(structures::contains))) {
            if (structureStart.getBoundingBox().isInside(blockPos)) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public StructureStart getStructureWithPieceAt(BlockPos blockPos, TagKey<Structure> structureTag) {
        return this.getStructureWithPieceAt(blockPos, structure -> structure.is(structureTag));
    }

    public StructureStart getStructureWithPieceAt(BlockPos blockPos, HolderSet<Structure> structures) {
        return this.getStructureWithPieceAt(blockPos, structures::contains);
    }

    public StructureStart getStructureWithPieceAt(BlockPos blockPos, Predicate<Holder<Structure>> predicate) {
        int sectionX = SectionPos.blockToSectionCoord(blockPos.getX());
        int sectionZ = SectionPos.blockToSectionCoord(blockPos.getZ());

        for (StructureStart structureStart : this.startsForStructure(sectionX, sectionZ, this.matching(predicate))) {
            if (this.structureHasPieceAt(blockPos, structureStart)) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }

    private Predicate<Structure> matching(Predicate<Holder<Structure>> predicate) {
        Registry<Structure> structures = this.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        return structure -> structures.get(structures.getId(structure)).map(predicate::test).orElse(false);
    }

    public StructureStart getStructureWithPieceAt(int x, int y, int z, Structure structure) {
        int sectionX = SectionPos.blockToSectionCoord(x);
        int sectionZ = SectionPos.blockToSectionCoord(z);

        for (StructureStart structureStart : this.startsForStructure(sectionX, sectionZ, structure)) {
            if (this.structureHasPieceAt(x, y, z, structureStart)) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public boolean structureHasPieceAt(BlockPos blockPos, StructureStart structureStart) {
        return this.structureHasPieceAt(blockPos.getX(), blockPos.getY(), blockPos.getZ(), structureStart);
    }

    public boolean structureHasPieceAt(int x, int y, int z, StructureStart structureStart) {
        for (StructurePiece piece : structureStart.getPieces()) {
            if (piece.getBoundingBox().isInside(x, y, z)) {
                return true;
            }
        }

        return false;
    }

    public Map<Structure, LongSet> getAllStructuresAt(BlockPos pos) {
        return this.getAllStructuresAt(pos.getX(), pos.getY(), pos.getZ());
    }

    public Map<Structure, LongSet> getAllStructuresAt(int x, int y, int z) {
        int sectionX = SectionPos.blockToSectionCoord(x);
        int sectionZ = SectionPos.blockToSectionCoord(z);
        return this.level.getChunk(sectionX, sectionZ, ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
    }

    public StructureCheckResult checkStructurePresence(ChunkPos pos, Structure structure, StructurePlacement placement, boolean createReference) {
        return this.structureCheck.checkStart(pos, structure, placement, createReference);
    }

    public void addReference(StructureStart start) {
        start.addReference();
        this.structureCheck.incrementReference(start.getChunkPos(), start.getStructure());
    }

    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }
}
