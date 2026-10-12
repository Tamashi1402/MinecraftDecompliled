package com.mojang.renderpearl.api.pipeline;

import com.mojang.renderpearl.util.UncheckedAutoCloseable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface CompiledRenderPipeline extends UncheckedAutoCloseable {
    boolean isClosed();

    @FunctionalInterface
    interface Pending {
        CompiledRenderPipeline.Pending NULL = () -> null;

        @Nullable CompiledRenderPipeline finishCompile();
    }
}
