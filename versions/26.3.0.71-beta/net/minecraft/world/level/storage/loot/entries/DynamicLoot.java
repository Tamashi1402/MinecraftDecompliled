package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class DynamicLoot extends SingleEntryContainerBase {
    public static final MapCodec<DynamicLoot> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(Identifier.CODEC.fieldOf("name").forGetter(e -> e.name)).and(uniformFields(i)).apply(i, DynamicLoot::new)
    );
    private final Identifier name;

    private DynamicLoot(Identifier name, int weight, int quality, Optional<Holder<LootItemCondition>> condition, Optional<Holder<LootItemFunction>> modifier) {
        super(weight, quality, condition, modifier);
        this.name = name;
    }

    @Override
    public MapCodec<DynamicLoot> codec() {
        return MAP_CODEC;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> output, LootContext context) {
        context.addDynamicDrops(this.name, output);
    }

    public static UniformContainerBase.Builder<?> dynamicEntry(Identifier name) {
        return simpleBuilder((weight, quality, conditions, functions) -> new DynamicLoot(name, weight, quality, conditions, functions));
    }
}
