package net.minecraft.world.clock;

import net.minecraft.core.Holder;

public interface ClockManager {
    ClockInstance getInstance(Holder<WorldClock> definition);

    default void setRate(Holder<WorldClock> definition, float rate) {
    }
}
