package com.mojang.renderpearl.backend.vulkan.init;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;

@OnlyIn(Dist.CLIENT)
public record VulkanFeature(VulkanPNextStruct struct, String name, long offset) {
    public VulkanFeature(VulkanPNextStruct struct, String name) {
        String structClassName = struct.pNextStructClass().getSimpleName();
        if (!structClassName.contains("Features")) {
            throw new IllegalArgumentException(
                "Struct name \""
                    + structClassName
                    + "\" does not contain \"Features\". All Vulkan features structs are expected to have \"Features\" in the name."
            );
        }

        this(struct, name, struct.fieldOffset(name));
    }

    public boolean get(VkPhysicalDeviceFeatures2 features2) {
        return this.get(features2.address());
    }

    public boolean get(long pNextChain) {
        long structAddr = this.struct.findStructInPNextChain(pNextChain);
        return structAddr == 0L ? false : this.getVkBool32(structAddr);
    }

    private boolean getVkBool32(long pointer) {
        return MemoryUtil.memGetInt(pointer + this.offset) != 0;
    }

    private void putVkBool32(boolean value, long structAddr) {
        MemoryUtil.memPutInt(structAddr + this.offset, value ? 1 : 0);
    }

    public boolean set(VkPhysicalDeviceFeatures2 features2, boolean value) {
        return this.set(features2.address(), value);
    }

    private boolean set(long pNextChain, boolean value) {
        long structAddr = this.struct.findStructInPNextChain(pNextChain);
        if (structAddr == 0L) {
            return false;
        }

        this.putVkBool32(value, structAddr);
        return true;
    }

    public void set(VkPhysicalDeviceFeatures2 features2, boolean value, MemoryStack stack) {
        this.set(features2.address(), value, stack);
    }

    public void set(long pNextChain, boolean value, MemoryStack stack) {
        long structAddr = this.struct.findOrCreateStructInPNextChain(pNextChain, stack);
        this.putVkBool32(value, structAddr);
    }

    @Override
    public String toString() {
        return this.struct + "." + this.name;
    }
}
