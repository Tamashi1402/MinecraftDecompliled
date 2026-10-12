package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.loot.LootContext;

public class AllOfCondition extends CompositeLootItemCondition {
    public static final MapCodec<AllOfCondition> MAP_CODEC = createCodec(AllOfCondition::new);

    private AllOfCondition(HolderSet<LootItemCondition> terms) {
        super(terms, combine(terms));
    }

    private static Predicate<LootContext> combine(HolderSet<LootItemCondition> terms) {
        return !terms.isBound() ? context -> {
            for (Holder<LootItemCondition> entry : terms) {
                if (!entry.value().test(context)) {
                    return false;
                }
            }

            return true;
        } : Util.allOf(holdersToLazyPredicates(terms));
    }

    public static AllOfCondition allOf(HolderSet<LootItemCondition> terms) {
        return new AllOfCondition(terms);
    }

    @Override
    public MapCodec<AllOfCondition> codec() {
        return MAP_CODEC;
    }

    public static AllOfCondition.Builder allOf(LootItemCondition.Builder... terms) {
        return new AllOfCondition.Builder(terms);
    }

    public static class Builder extends CompositeLootItemCondition.Builder {
        public Builder(LootItemCondition.Builder... terms) {
            super(terms);
        }

        @Override
        public AllOfCondition.Builder and(LootItemCondition.Builder term) {
            this.addTerm(term);
            return this;
        }

        @Override
        protected LootItemCondition create(HolderSet<LootItemCondition> terms) {
            return new AllOfCondition(terms);
        }
    }
}
