package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetItemFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetItemFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> commonFields(i).and(Item.CODEC.fieldOf("item").forGetter(f -> f.item)).apply(i, SetItemFunction::new)
    );
    private final Holder<Item> item;

    public SetItemFunction(Optional<Holder<LootItemCondition>> condition, Holder<Item> item) {
        super(condition);
        this.item = item;
    }

    @Override
    public MapCodec<SetItemFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        return itemStack.transmuteCopy(this.item.value());
    }
}
