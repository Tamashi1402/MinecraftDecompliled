package com.mojang.renderpearl.backend.vulkan.init;

import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.backend.vulkan.VulkanUtils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;

@OnlyIn(Dist.CLIENT)
public record FeatureSet(String name, Set<String> extensions, Set<VulkanFeature> features, FeatureSet.Condition condition) {
    public FeatureSet(String name, Set<String> extensions, Set<VulkanFeature> features, FeatureSet.Condition condition) {
        this.name = name;
        this.extensions = Set.copyOf(extensions);
        this.features = Set.copyOf(features);
        this.condition = condition;
    }

    public FeatureSet(String name, Set<String> extensions, Set<VulkanFeature> features) {
        this(name, extensions, features, FeatureSet.Condition.IDENTITY_CONDITION);
    }

    public FeatureSet(String name, Collection<FeatureSet> others) {
        FeatureSet.Condition condition = others.stream()
            .map(FeatureSet::condition)
            .reduce(FeatureSet.Condition.IDENTITY_CONDITION, FeatureSet.Condition::reduce);
        Set<String> extensions = new ObjectOpenHashSet<>();
        Set<VulkanFeature> features = new ObjectOpenHashSet<>();

        for (FeatureSet other : others) {
            extensions.addAll(other.extensions);
            features.addAll(other.features);
        }

        this(name, extensions, features, condition);
    }

    public FeatureSet composite(FeatureSet other) {
        return new FeatureSet(this.name, List.of(this, other));
    }

    public boolean contains(FeatureSet other) {
        return this.extensions.containsAll(other.extensions) && this.features.containsAll(other.features);
    }

    public boolean checkCondition(VkPhysicalDevice device) throws BackendCreationException {
        return this.condition.test(device);
    }

    public boolean isSupported(VkPhysicalDevice vkPhysicalDevice) throws BackendCreationException {
        return this.isSupported(vkPhysicalDevice, VulkanUtils.enumerateExtensions(vkPhysicalDevice));
    }

    public boolean isSupported(VkPhysicalDevice vkPhysicalDevice, Set<String> deviceSupportedExtensions) throws BackendCreationException {
        return !deviceSupportedExtensions.containsAll(this.extensions) ? false : this.allFeaturesSupported(vkPhysicalDevice);
    }

    public boolean allFeaturesSupported(VkPhysicalDevice vkPhysicalDevice) {
        if (!this.features.isEmpty()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkPhysicalDeviceFeatures2 supportedFeatures = VulkanUtils.enumerateFeatures(vkPhysicalDevice, this.features, stack);

                for (VulkanFeature feature : this.features) {
                    if (!feature.get(supportedFeatures)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public Set<String> unsupportedExtensions(Set<String> deviceSupportedExtensions) {
        if (this.extensions.isEmpty()) {
            return Set.of();
        }

        Set<String> supportedExtensions = new ObjectOpenHashSet<>(this.extensions);
        supportedExtensions.removeAll(deviceSupportedExtensions);
        return supportedExtensions.isEmpty() ? Set.of() : Collections.unmodifiableSet(supportedExtensions);
    }

    public Set<String> supportedExtensions(Set<String> deviceSupportedExtensions) {
        if (this.extensions.isEmpty()) {
            return Set.of();
        }

        Set<String> supportedExtensions = new ObjectOpenHashSet<>();

        for (String extension : this.extensions) {
            if (deviceSupportedExtensions.contains(extension)) {
                supportedExtensions.add(extension);
            }
        }

        return supportedExtensions.isEmpty() ? Set.of() : Collections.unmodifiableSet(supportedExtensions);
    }

    public Set<VulkanFeature> unsupportedFeatures(VkPhysicalDevice vkPhysicalDevice) {
        Set<VulkanFeature> unsupportedFeatures = new ObjectOpenHashSet<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceFeatures2 featuresChain = VulkanUtils.enumerateFeatures(vkPhysicalDevice, this.features, stack);

            for (VulkanFeature feature : this.features) {
                if (!feature.get(featuresChain)) {
                    unsupportedFeatures.add(feature);
                }
            }
        }

        return unsupportedFeatures.isEmpty() ? Set.of() : Collections.unmodifiableSet(unsupportedFeatures);
    }

    public Set<VulkanFeature> supportedFeatures(VkPhysicalDevice vkPhysicalDevice) {
        Set<VulkanFeature> supportedFeatures = new ObjectOpenHashSet<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceFeatures2 featuresChain = VulkanUtils.enumerateFeatures(vkPhysicalDevice, this.features, stack);

            for (VulkanFeature feature : this.features) {
                if (feature.get(featuresChain)) {
                    supportedFeatures.add(feature);
                }
            }
        }

        return supportedFeatures.isEmpty() ? Set.of() : Collections.unmodifiableSet(supportedFeatures);
    }

    public interface Condition {
        FeatureSet.Condition IDENTITY_CONDITION = var0 -> true;

        static FeatureSet.Condition reduce(FeatureSet.Condition a, FeatureSet.Condition b) {
            if (a == b) {
                return a;
            } else if (a == IDENTITY_CONDITION) {
                return b;
            } else {
                return b == IDENTITY_CONDITION ? a : device -> a.test(device) && b.test(device);
            }
        }

        boolean test(VkPhysicalDevice device) throws BackendCreationException;
    }
}
