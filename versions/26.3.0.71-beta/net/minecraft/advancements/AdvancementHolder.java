package net.minecraft.advancements;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public record AdvancementHolder(Identifier id, Advancement value) {
    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancementHolder> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, AdvancementHolder::id, Advancement.STREAM_CODEC, AdvancementHolder::value, AdvancementHolder::new
    );

    public void register(BootstrapContext<Advancement> output) {
        this.register(output, new net.neoforged.neoforge.common.conditions.ICondition[0]);
    }

    public void register(BootstrapContext<Advancement> output, net.neoforged.neoforge.common.conditions.ICondition... conditions) {
        output.register(ResourceKey.create(Registries.ADVANCEMENT, this.id), this.value, conditions);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj ? true : obj instanceof AdvancementHolder holder && this.id.equals(holder.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
