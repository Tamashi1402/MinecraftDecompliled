package net.minecraft.util;

import java.util.function.Function;

public interface BoundedFloatFunction<C> {
    BoundedFloatFunction<Float> IDENTITY = new BoundedFloatFunction<Float>() {
        public float apply(Float value) {
            return value;
        }

        @Override
        public Interval range() {
            return Interval.INFINITE;
        }
    };

    float apply(final C c);

    Interval range();

    static <C> BoundedFloatFunction<C> constant(float value) {
        final Interval range = Interval.ofExact(value);
        return new BoundedFloatFunction<C>() {
            @Override
            public float apply(C c) {
                return value;
            }

            @Override
            public Interval range() {
                return range;
            }
        };
    }

    default <C2> BoundedFloatFunction<C2> comap(Function<C2, C> function) {
        final BoundedFloatFunction<C> outer = this;
        return new BoundedFloatFunction<C2>() {
            @Override
            public float apply(C2 c2) {
                return outer.apply(function.apply(c2));
            }

            @Override
            public Interval range() {
                return outer.range();
            }
        };
    }
}
