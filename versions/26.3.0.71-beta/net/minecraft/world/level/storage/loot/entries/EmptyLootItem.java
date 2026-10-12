package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class EmptyLootItem extends SingleEntryContainerBase {
    public static final MapCodec<EmptyLootItem> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> uniformFields(i).apply(i, EmptyLootItem::new));

    private EmptyLootItem(int weight, int quality, Optional<Holder<LootItemCondition>> condition, Optional<Holder<LootItemFunction>> modifier) {
        super(weight, quality, condition, modifier);
    }

    @Override
    public MapCodec<EmptyLootItem> codec() {
        return MAP_CODEC;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> output, LootContext context) {
    }

    public static UniformContainerBase.Builder<?> emptyItem() {
        return simpleBuilder(EmptyLootItem::new);
    }
}
