package net.minecraft.client.renderer.oit;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record OitPipelineSet(RenderPipeline depthBoundsPipeline, RenderPipeline transmittancePipeline, RenderPipeline accumulatePipeline) {
    public RenderPipeline getPipeline(OitStage stage) {
        return switch (stage) {
            case DEPTH_BOUNDS -> this.depthBoundsPipeline;
            case TRANSMITTANCE -> this.transmittancePipeline;
            case ACCUMULATE -> this.accumulatePipeline;
            default -> throw new IllegalArgumentException("Unsupported OIT stage.");
        };
    }

    /// @deprecated Neo: use [#builder(net.minecraft.resources.Identifier, RenderPipeline.Builder)] instead
    @Deprecated
    public static OitPipelineSet.Builder builder(String locationSuffix, RenderPipeline.Builder builder) {
        return builder(net.minecraft.resources.Identifier.withDefaultNamespace(locationSuffix), builder);
    }

    public static OitPipelineSet.Builder builder(net.minecraft.resources.Identifier locationSuffix, RenderPipeline.Builder builder) {
        return new OitPipelineSet.Builder(builder.buildSnippet(), locationSuffix);
    }

    public static class Builder {
        private static final Consumer<RenderPipeline.Builder> DISABLE_DEPTH_TEST = builder -> builder.withDepthStencilState(Optional.empty());
        private final RenderPipeline.Snippet baseSnippet;
        private final String locationSuffix;
        private final net.minecraft.resources.Identifier locationSuffixId;
        private Optional<Consumer<RenderPipeline.Builder>> depthBoundsModifier = Optional.empty();
        private Optional<Consumer<RenderPipeline.Builder>> transmittanceModifier = Optional.empty();
        private Optional<Consumer<RenderPipeline.Builder>> accumulateModifier = Optional.empty();

        /// @deprecated Neo: use [#Builder(RenderPipeline.Snippet, net.minecraft.resources.Identifier)] instead
        @Deprecated
        public Builder(RenderPipeline.Snippet baseSnippet, String locationSuffix) {
            this.baseSnippet = baseSnippet;
            this.locationSuffix = locationSuffix;
            this.locationSuffixId = net.minecraft.resources.Identifier.withDefaultNamespace(locationSuffix);
        }

        public Builder(RenderPipeline.Snippet baseSnippet, net.minecraft.resources.Identifier locationSuffix) {
            this.baseSnippet = baseSnippet;
            this.locationSuffix = locationSuffix.toShortString();
            this.locationSuffixId = locationSuffix;
        }

        public OitPipelineSet.Builder withDepthBoundsModifier(Consumer<RenderPipeline.Builder> modifier) {
            this.depthBoundsModifier = composeModifiers(this.depthBoundsModifier, modifier);
            return this;
        }

        public OitPipelineSet.Builder withTransmittanceModifier(Consumer<RenderPipeline.Builder> modifier) {
            this.transmittanceModifier = composeModifiers(this.transmittanceModifier, modifier);
            return this;
        }

        public OitPipelineSet.Builder withAccumulateModifier(Consumer<RenderPipeline.Builder> modifier) {
            this.accumulateModifier = composeModifiers(this.accumulateModifier, modifier);
            return this;
        }

        public OitPipelineSet.Builder withoutDepthTest() {
            return this.withDepthBoundsModifier(DISABLE_DEPTH_TEST).withTransmittanceModifier(DISABLE_DEPTH_TEST).withAccumulateModifier(DISABLE_DEPTH_TEST);
        }

        private static Optional<Consumer<RenderPipeline.Builder>> composeModifiers(
            Optional<Consumer<RenderPipeline.Builder>> currentModifier, Consumer<RenderPipeline.Builder> newModifier
        ) {
            return currentModifier.isPresent() ? Optional.of(builder -> {
                currentModifier.get().accept(builder);
                newModifier.accept(builder);
            }) : Optional.of(newModifier);
        }

        public OitPipelineSet build() {
            RenderPipeline.Builder depthBoundsBuilder = RenderPipeline.builder(this.baseSnippet, RenderPipelines.OIT_DEPTH_BOUNDS_SNIPPET)
                .withLocation(this.locationSuffixId.withPrefix("pipeline/oit_depth_bounds_"));
            this.depthBoundsModifier.ifPresent(modifier -> modifier.accept(depthBoundsBuilder));
            RenderPipeline.Builder transmittanceBuilder = RenderPipeline.builder(this.baseSnippet, RenderPipelines.OIT_TRANSMITTANCE_SNIPPET)
                .withLocation(this.locationSuffixId.withPrefix("pipeline/oit_transmittance_"));
            this.transmittanceModifier.ifPresent(modifier -> modifier.accept(transmittanceBuilder));
            RenderPipeline.Builder accumulateBuilder = RenderPipeline.builder(this.baseSnippet, RenderPipelines.OIT_ACCUMULATE_SNIPPET)
                .withLocation(this.locationSuffixId.withPrefix("pipeline/oit_accumulate_"));
            this.accumulateModifier.ifPresent(modifier -> modifier.accept(accumulateBuilder));
            return new OitPipelineSet(depthBoundsBuilder.build(), transmittanceBuilder.build(), accumulateBuilder.build());
        }
    }
}
