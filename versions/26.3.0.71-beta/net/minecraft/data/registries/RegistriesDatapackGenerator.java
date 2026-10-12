package net.minecraft.data.registries;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;

/// @deprecated Neo: Use [net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider] instead
@Deprecated
public class RegistriesDatapackGenerator implements DataProvider {
    private final PackOutput output;
    private final String name;
    private final Collection<RegistryDataLoader.RegistryData<?>> registryData;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private final java.util.function.Predicate<String> namespacePredicate;
    private final CompletableFuture<net.neoforged.neoforge.common.data.RegistriesWithConditions> registriesWithConditions;

    /// @deprecated Neo: use [#RegistriesDatapackGenerator(PackOutput, String, Collection, CompletableFuture, java.util.Set)] instead
    @Deprecated
    public RegistriesDatapackGenerator(
        PackOutput output, String name, Collection<RegistryDataLoader.RegistryData<?>> registryData, CompletableFuture<HolderLookup.Provider> registryContents
    ) {
        this(output, name, registryData, registryContents.thenApply(net.neoforged.neoforge.common.data.RegistriesWithConditions::new), null);
    }

    public RegistriesDatapackGenerator(
        PackOutput output, String name, Collection<RegistryDataLoader.RegistryData<?>> registryData, CompletableFuture<net.neoforged.neoforge.common.data.RegistriesWithConditions> registryContents, java.util.@org.jspecify.annotations.Nullable Set<String> modIds
    ) {
        this.name = name;
        this.registryData = registryData;
        this.registries = registryContents.thenApply(net.neoforged.neoforge.common.data.RegistriesWithConditions::registries);
        this.output = output;
        this.namespacePredicate = modIds == null ? _ -> true : modIds::contains;
        this.registriesWithConditions = registryContents;
    }

    public static RegistriesDatapackGenerator forWorldLayer(PackOutput output, CompletableFuture<HolderLookup.Provider> registryContents) {
        return new RegistriesDatapackGenerator(output, "world", net.neoforged.neoforge.registries.DataPackRegistriesHooks.getWorldRegistriesWithDimensions().toList(), registryContents);
    }

    public static DataProvider forReloadableLayer(PackOutput output, CompletableFuture<HolderLookup.Provider> registryContents) {
        return new RegistriesDatapackGenerator(output, "reloadable", net.neoforged.neoforge.registries.DataPackRegistriesHooks.getReloadableRegistries(), registryContents);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return this.registriesWithConditions
            .thenCompose(
                access -> {
                    DynamicOps<JsonElement> registryOps = access.createSerializationContext(JsonOps.INSTANCE);
                    return CompletableFuture.allOf(
                        this.registryData
                            .stream()
                            .flatMap(v -> this.dumpRegistryCap(cache, access, registryOps, (RegistryDataLoader.RegistryData<?>)v).stream())
                            .toArray(CompletableFuture[]::new)
                    );
                }
            );
    }

    /// @deprecated Neo: use [#dumpRegistryCap(CachedOutput, net.neoforged.neoforge.common.data.RegistriesWithConditions, DynamicOps, RegistryDataLoader.RegistryData)] instead
    @Deprecated
    private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
        CachedOutput cache, HolderLookup.Provider registries, DynamicOps<JsonElement> writeOps, RegistryDataLoader.RegistryData<T> v
    ) {
        return this.dumpRegistryCap(cache, new net.neoforged.neoforge.common.data.RegistriesWithConditions(registries), writeOps, v);
    }

    private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
        CachedOutput cache, net.neoforged.neoforge.common.data.RegistriesWithConditions registries, DynamicOps<JsonElement> writeOps, RegistryDataLoader.RegistryData<T> v
    ) {
        ResourceKey<? extends Registry<T>> registryKey = v.key();
        var conditionalCodec = net.neoforged.neoforge.common.conditions.ConditionalOps.createConditionalCodecWithConditions(v.elementCodec());
        var conditions = registries.conditionsByRegistry().getOrDefault(registryKey, java.util.Map.of());
        return registries.lookup(registryKey)
            .map(
                registry -> {
                    PackOutput.PathProvider pathProvider = this.output.createRegistryElementsPathProvider(registryKey);
                    return CompletableFuture.allOf(
                        registry.listElements()
                            .filter(holder -> this.namespacePredicate.test(holder.key().identifier().getNamespace()))
                            .map(e -> (CompletableFuture<Object>) dumpValue(pathProvider.json(e.key().identifier()), cache, writeOps, conditionalCodec, Optional.of(new net.neoforged.neoforge.common.conditions.WithConditions<>(conditions.getOrDefault(e.key(), java.util.List.of()), e.value()))))
                            .toArray(CompletableFuture[]::new)
                    );
                }
            );
    }

    private static <E> CompletableFuture<?> dumpValue(Path path, CachedOutput cache, DynamicOps<JsonElement> ops, Encoder<java.util.Optional<net.neoforged.neoforge.common.conditions.WithConditions<E>>> codec, java.util.Optional<net.neoforged.neoforge.common.conditions.WithConditions<E>> value) {
        return codec.encodeStart(ops, value)
            .mapOrElse(
                result -> DataProvider.saveStable(cache, result, path),
                error -> CompletableFuture.failedFuture(new IllegalStateException("Couldn't generate file '" + path + "': " + error.message()))
            );
    }

    @Override
    public String getName() {
        return "Registries for " + this.name;
    }
}
