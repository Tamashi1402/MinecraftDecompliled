package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class TagEntry extends ExpandableContainerBase {
    public static final MapCodec<TagEntry> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(RegistryCodecs.holderSet(Registries.ITEM).fieldOf("items").forGetter(e -> e.tag)).and(expandableFields(i)).apply(i, TagEntry::new)
    );
    private final HolderSet<Item> tag;

    private TagEntry(
        HolderSet<Item> tag,
        boolean expand,
        int weight,
        int quality,
        Optional<Holder<LootItemCondition>> condition,
        Optional<Holder<LootItemFunction>> modifier
    ) {
        super(expand, weight, quality, condition, modifier);
        this.tag = tag;
    }

    @Override
    public MapCodec<TagEntry> codec() {
        return MAP_CODEC;
    }

    @Override
    protected boolean addExpandedEntries(Consumer<LootPoolEntry> output) {
        for (final Holder<Item> item : this.tag) {
            output.accept(new UniformContainerBase.EntryBase() {
                @Override
                public void createItemStack(Consumer<ItemStack> output, LootContext context) {
                    output.accept(new ItemStack(item));
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
                TagEntry.this.tag.forEach(item -> output.accept(new ItemStack((Holder<Item>)item)));
            }
        });
        return true;
    }

    public static UniformContainerBase.Builder<?> tagContents(HolderSet<Item> tag) {
        return simpleBuilder((weight, quality, conditions, functions) -> new TagEntry(tag, false, weight, quality, conditions, functions));
    }

    public static UniformContainerBase.Builder<?> expandTag(HolderSet<Item> tag) {
        return simpleBuilder((weight, quality, conditions, functions) -> new TagEntry(tag, true, weight, quality, conditions, functions));
    }
}
