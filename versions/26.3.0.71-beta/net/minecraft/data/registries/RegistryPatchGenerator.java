package net.minecraft.data.registries;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Cloner;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootDataType;

public class RegistryPatchGenerator {
    private static boolean hasAnyPatchedElement(RegistrySetBuilder.PatchedRegistries newRegistries, ResourceKey<? extends Registry<?>> registry) {
        return newRegistries.patches().lookup(registry).flatMap(lookup -> lookup.listElements().findAny()).isPresent();
    }

    public static CompletableFuture<RegistrySetBuilder.PatchedRegistries> createWorldLookup(
        CompletableFuture<HolderLookup.Provider> vanilla, RegistrySetBuilder packBuilder
    ) {
        return vanilla.thenApply(parent -> {
            RegistryAccess.Frozen staticRegistries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            Cloner.Factory cloner = new Cloner.Factory();
            net.neoforged.neoforge.registries.DataPackRegistriesHooks.getWorldRegistriesWithDimensions().forEach(registryData -> registryData.runWithArguments(cloner::addCodec));
            RegistrySetBuilder.PatchedRegistries newRegistries = packBuilder.buildPatch(staticRegistries, parent, cloner);
            boolean hasAnyPatchedBiomes = hasAnyPatchedElement(newRegistries, Registries.BIOME);
            boolean hasAnyPatchedFeatures = hasAnyPatchedElement(newRegistries, Registries.PLACED_FEATURE);
            if (hasAnyPatchedBiomes || hasAnyPatchedFeatures) {
                VanillaRegistries.validateThatAllBiomeFeaturesHaveBiomeFilter(newRegistries.full());
            }

            return newRegistries;
        });
    }

    public static CompletableFuture<RegistrySetBuilder.PatchedRegistries> createReloadableLookup(
        CompletableFuture<HolderLookup.Provider> contextFuture, CompletableFuture<HolderLookup.Provider> vanillaFuture, RegistrySetBuilder packBuilder
    ) {
        return contextFuture.thenCombine(vanillaFuture, (context, vanilla) -> {
            Cloner.Factory cloner = new Cloner.Factory();
            net.neoforged.neoforge.registries.DataPackRegistriesHooks.getReloadableRegistries().forEach(registryData -> registryData.runWithArguments(cloner::addCodec));
            RegistrySetBuilder.PatchedRegistries newRegistries = packBuilder.buildPatch(context, vanilla, cloner);
            boolean hasAnyPatchedLootData = LootDataType.values().anyMatch(type -> hasAnyPatchedElement(newRegistries, type.registryKey()));
            if (hasAnyPatchedLootData) {
                VanillaRegistries.validateLootData(newRegistries.full());
            }

            return newRegistries;
        });
    }
}
