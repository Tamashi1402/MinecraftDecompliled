package net.minecraft.data.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

import net.neoforged.neoforge.common.extensions.BootstrapContextExtension;

public interface BootstrapContext<T> extends BootstrapContextAccess, BootstrapContextExtension<T> {
    Holder.Reference<T> register(ResourceKey<T> key, T value);
}
