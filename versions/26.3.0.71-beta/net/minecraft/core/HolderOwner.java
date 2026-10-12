package net.minecraft.core;

public interface HolderOwner<T> {
    default boolean canSerialize(HolderOwner<T> owner) {
        return owner == this;
    }
}
