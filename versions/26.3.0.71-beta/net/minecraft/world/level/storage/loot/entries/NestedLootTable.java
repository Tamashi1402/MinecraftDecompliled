package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class NestedLootTable extends ExpandableContainerBase {
    public static final MapCodec<NestedLootTable> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(LootTable.LIST_CODEC.fieldOf("value").forGetter(e -> e.value)).and(expandableFields(i)).apply(i, NestedLootTable::new)
    );
    private final HolderSet<LootTable> value;

    private NestedLootTable(
        HolderSet<LootTable> value,
        boolean expand,
        int weight,
        int quality,
        Optional<Holder<LootItemCondition>> condition,
        Optional<Holder<LootItemFunction>> modifier
    ) {
        super(expand, weight, quality, condition, modifier);
        this.value = value;
    }

    @Override
    public MapCodec<NestedLootTable> codec() {
        return MAP_CODEC;
    }

    @Override
    protected boolean addExpandedEntries(Consumer<LootPoolEntry> output) {
        for (final Holder<LootTable> table : this.value) {
            output.accept(new UniformContainerBase.EntryBase() {
                @Override
                public void createItemStack(Consumer<ItemStack> output, LootContext context) {
                    table.value().getRandomItemsRaw(context, output);
                }
            });
        }

        return true;
    }

    @Override
    protected boolean addUnexpandedEntry(Consumer<LootPoolEntry> output) {
        output.accept(new UniformContainerBase.EntryBase() {
            @Override
            public void createItemStack(Consumer<ItemStack> output, LootContext context) {
                NestedLootTable.this.value.forEach(t -> t.value().getRandomItemsRaw(context, output));
            }
        });
        return true;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validateHolderSet(context, "value", this.value);
    }

    public static UniformContainerBase.Builder<?> lootTableReference(Holder<LootTable> table) {
        return simpleBuilder(
            (weight, quality, conditions, functions) -> new NestedLootTable(HolderSet.direct(table), false, weight, quality, conditions, functions)
        );
    }

    public static UniformContainerBase.Builder<?> inlineLootTable(LootTable table) {
        return lootTableReference(Holder.direct(table));
    }

    public static UniformContainerBase.Builder<?> tableSet(HolderSet<LootTable> tables) {
        return simpleBuilder((wheight, quality, conditions, functions) -> new NestedLootTable(tables, false, wheight, quality, conditions, functions));
    }
}
