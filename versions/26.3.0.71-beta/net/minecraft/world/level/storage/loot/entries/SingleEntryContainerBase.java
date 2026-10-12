package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class SingleEntryContainerBase extends UniformContainerBase {
    private final LootPoolEntry entry = new UniformContainerBase.EntryBase() {
        @Override
        public void createItemStack(Consumer<ItemStack> output, LootContext context) {
            SingleEntryContainerBase.this.createItemStack(output, context);
        }
    };

    protected SingleEntryContainerBase(int weight, int quality, Optional<Holder<LootItemCondition>> condition, Optional<Holder<LootItemFunction>> modifier) {
        super(weight, quality, condition, modifier);
    }

    @Override
    public abstract MapCodec<? extends SingleEntryContainerBase> codec();

    protected abstract void createItemStack(Consumer<ItemStack> output, LootContext context);

    @Override
    public final boolean expandRaw(LootContext context, Consumer<LootPoolEntry> output) {
        output.accept(this.entry);
        return true;
    }
}
