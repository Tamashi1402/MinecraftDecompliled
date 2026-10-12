package net.minecraft.world.level.blockscan;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jspecify.annotations.Nullable;

public class FilteredSectionCache {
    private final LevelReader level;
    private final FilteredSectionCache.@Nullable CacheEntry[] sectionCache;
    private final Predicate<BlockState> statePredicate;
    private final java.util.function.BiPredicate<BlockState, BlockPos> positionPredicate;

    /// Neo: Add overload which accepts a position-based predicate (evaluated only if the state predicate accepts)
    public FilteredSectionCache(LevelReader level, Predicate<BlockState> statePredicate, java.util.function.BiPredicate<BlockState, BlockPos> positionPredicate) {
        this.level = level;
        this.statePredicate = statePredicate;
        this.positionPredicate = positionPredicate;
        this.sectionCache = new FilteredSectionCache.CacheEntry[8];
    }

    public FilteredSectionCache(LevelReader level, Predicate<BlockState> statePredicate) {
        this(level, statePredicate, BlockMatcher.NO_POSITION_PREDICATE);
    }

    public @Nullable BlockState getBlockState(BlockPos pos) {
        int sectionX = SectionPos.blockToSectionCoord(pos.getX());
        int sectionY = SectionPos.blockToSectionCoord(pos.getY());
        int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());
        if (sectionY >= this.level.getMinSectionY() && sectionY <= this.level.getMaxSectionY()) {
            LevelChunkSection section = this.getOrLoad(sectionX, sectionY, sectionZ);
            if (section == null) {
                return null;
            }

            int relativeX = SectionPos.sectionRelative(pos.getX());
            int relativeY = SectionPos.sectionRelative(pos.getY());
            int relativeZ = SectionPos.sectionRelative(pos.getZ());
            BlockState blockState = section.getBlockState(relativeX, relativeY, relativeZ);
            return this.statePredicate.test(blockState) && this.positionPredicate.test(blockState, pos) ? blockState : null;
        } else {
            BlockState air = Blocks.AIR.defaultBlockState();
            return this.statePredicate.test(air) && this.positionPredicate.test(air, pos) ? air : null;
        }
    }

    private @Nullable LevelChunkSection getOrLoad(int sectionX, int sectionY, int sectionZ) {
        int hash = this.getHash(sectionX, sectionY, sectionZ);
        FilteredSectionCache.CacheEntry cacheEntry = this.sectionCache[hash];
        if (cacheEntry == null || cacheEntry.sectionX != sectionX || cacheEntry.sectionY != sectionY || cacheEntry.sectionZ != sectionZ) {
            ChunkAccess chunk = this.level.getChunk(sectionX, sectionZ);
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
            cacheEntry = new FilteredSectionCache.CacheEntry(section.maybeHas(this.statePredicate) ? section : null, sectionX, sectionY, sectionZ);
            this.sectionCache[hash] = cacheEntry;
        }

        return cacheEntry.chunkSection;
    }

    private int getHash(int sectionX, int sectionY, int sectionZ) {
        int relativeX = sectionX & 1;
        int relativeY = sectionY & 1;
        int relativeZ = sectionZ & 1;
        return ((relativeZ << 1) + relativeY << 1) + relativeX;
    }

    private record CacheEntry(@Nullable LevelChunkSection chunkSection, int sectionX, int sectionY, int sectionZ) {
    }
}
