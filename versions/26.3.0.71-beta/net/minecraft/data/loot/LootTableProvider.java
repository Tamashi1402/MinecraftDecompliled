package net.minecraft.data.loot;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.storage.loot.LootTable;

public class LootTableProvider implements SingleRegistryBootstrap<LootTable> {
    private final Set<ResourceKey<LootTable>> requiredTables;
    private final List<LootTableProvider.SubProviderEntry> subProviders;

    public LootTableProvider(Set<ResourceKey<LootTable>> requiredTables, List<LootTableProvider.SubProviderEntry> subProviders) {
        this.subProviders = subProviders;
        this.requiredTables = requiredTables;
    }

    @Override
    public void run(BootstrapContext<LootTable> context) {
        Map<RandomSupport.Seed128bit, Identifier> randomSequenceSeeds = new Object2ObjectOpenHashMap<>();
        HolderGetter<LootTable> lootTables = context.lookup(Registries.LOOT_TABLE);
        this.requiredTables.forEach(lootTables::get);
        this.subProviders.forEach(subProvider -> subProvider.bootstrap().create(new LootTableSubProvider.Context() {
            @Override
            public Holder.Reference<LootTable> accept(ResourceKey<LootTable> key, LootTable.Builder lootTable) {
                Identifier sequenceId = LootTableProvider.sequenceIdForLootTable(key);
                Identifier previous = randomSequenceSeeds.put(RandomSequence.seedForKey(sequenceId), sequenceId);
                if (previous != null) {
                    Util.logAndPauseIfInIde("Loot table random sequence seed collision on " + previous + " and " + key.identifier());
                }

                LootTable table = lootTable.setRandomSequence(sequenceId).setParamSet(subProvider.paramSet).build();
                return context.register(key, table, lootTable.buildConditions().toArray(net.neoforged.neoforge.common.conditions.ICondition[]::new));
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
                return context.lookup(key);
            }

            @Deprecated
            @Override
            public <S> Stream<Holder.Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> key) {
                return context.listContextElements(key);
            }
        }).run());
    }

    private static Identifier sequenceIdForLootTable(ResourceKey<LootTable> id) {
        return id.identifier();
    }

    public record SubProviderEntry(LootTableSubProvider.Factory bootstrap, ContextKeySet paramSet) {
    }
}
