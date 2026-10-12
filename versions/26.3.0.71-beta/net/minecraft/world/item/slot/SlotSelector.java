package net.minecraft.world.item.slot;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

public sealed interface SlotSelector permits SlotSelector.FilteringSelector, SlotSelector.TrackingSelector {
    SlotSelector ANY_SLOT = new SlotSelector.FilteringSelector(null);
    SlotSelector EMPTY_SLOTS = selecting(ItemStack::isEmpty);
    SlotSelector NON_EMPTY_SLOTS = selecting(s -> !s.isEmpty());

    boolean trySelectSlot(ItemStack itemInSlot);

    SlotSelector filter(Predicate<? super ItemStack> predicate);

    SlotSelector limit(int limit);

    static SlotSelector selecting(Predicate<? super ItemStack> predicate) {
        return new SlotSelector.FilteringSelector(predicate);
    }

    static SlotSelector tracking(SlotSelector slotSelector, MutableInt selectedCount) {
        return new SlotSelector.TrackingSelector(slotSelector, Integer.MAX_VALUE, selectedCount);
    }

    static SlotSelector tracking(MutableInt selectedCount) {
        return tracking(ANY_SLOT, selectedCount);
    }

    record FilteringSelector(@Nullable Predicate<? super ItemStack> filter) implements SlotSelector {
        @Override
        public boolean trySelectSlot(ItemStack itemInSlot) {
            return this.filter == null || this.filter.test(itemInSlot);
        }

        @Override
        public SlotSelector filter(Predicate<? super ItemStack> predicate) {
            return this.filter == null
                ? new SlotSelector.FilteringSelector(predicate)
                : new SlotSelector.FilteringSelector(t -> this.filter.test(t) && predicate.test(t));
        }

        @Override
        public SlotSelector limit(int limit) {
            return new SlotSelector.TrackingSelector(this, limit, new MutableInt());
        }
    }

    record TrackingSelector(SlotSelector slotSelector, int limit, MutableInt selectedCount) implements SlotSelector {
        @Override
        public boolean trySelectSlot(ItemStack itemInSlot) {
            if (this.selectedCount.intValue() < this.limit && this.slotSelector.trySelectSlot(itemInSlot)) {
                this.selectedCount.increment();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public SlotSelector filter(Predicate<? super ItemStack> predicate) {
            return new SlotSelector.TrackingSelector(this.slotSelector.filter(predicate), this.limit, this.selectedCount);
        }

        @Override
        public SlotSelector limit(int limit) {
            return limit < this.limit ? new SlotSelector.TrackingSelector(this.slotSelector, limit, this.selectedCount) : this;
        }
    }
}
