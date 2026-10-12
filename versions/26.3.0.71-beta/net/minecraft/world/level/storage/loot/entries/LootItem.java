package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItem extends SingleEntryContainerBase {
    public static final MapCodec<LootItem> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(Item.CODEC.fieldOf("name").forGetter(e -> e.item)).and(uniformFields(i)).apply(i, LootItem::new)
    );
    private final Holder<Item> item;

    private LootItem(Holder<Item> item, int weight, int quality, Optional<Holder<LootItemCondition>> condition, Optional<Holder<LootItemFunction>> modifier) {
        super(weight, quality, condition, modifier);
        this.item = item;
    }

    @Override
    public MapCodec<LootItem> codec() {
        return MAP_CODEC;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> output, LootContext context) {
        output.accept(new ItemStack(this.item));
    }

    public static UniformContainerBase.Builder<?> lootTableItem(ItemLike item) {
        return simpleBuilder(
            (weight, quality, conditions, functions) -> new LootItem(item.asItem().builtInRegistryHolder(), weight, quality, conditions, functions)
        );
    }
}
