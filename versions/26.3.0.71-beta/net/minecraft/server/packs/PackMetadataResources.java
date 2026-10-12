package net.minecraft.server.packs;

import java.io.IOException;
import java.io.InputStream;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.common.extensions.PackMetadataResourcesExtension;

public interface PackMetadataResources extends AutoCloseable, PackMetadataResourcesExtension {
    PackLocationInfo location();

    @Nullable IoSupplier<InputStream> getRootResource(String... path);

    <T> @Nullable T getMetadataSection(MetadataSectionType<T> metadataSerializer) throws IOException;

    @Override
    void close();
}
