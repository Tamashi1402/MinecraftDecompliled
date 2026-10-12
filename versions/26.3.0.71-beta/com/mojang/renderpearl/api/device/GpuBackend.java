package com.mojang.renderpearl.api.device;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface GpuBackend {
    String getName();

    void loadLibrary() throws BackendCreationException;

    void unloadLibrary();

    long createWindow(@Nullable String title, int width, int height, long flags);

    GpuDevice createDevice(GpuDebugOptions debugOptions) throws BackendCreationException;
}
