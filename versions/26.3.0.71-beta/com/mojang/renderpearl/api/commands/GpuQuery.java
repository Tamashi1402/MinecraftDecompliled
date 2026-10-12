package com.mojang.renderpearl.api.commands;

import com.mojang.renderpearl.util.UncheckedAutoCloseable;
import java.util.OptionalLong;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GpuQuery extends UncheckedAutoCloseable {
    OptionalLong getValue();

    @Override
    void close();
}
