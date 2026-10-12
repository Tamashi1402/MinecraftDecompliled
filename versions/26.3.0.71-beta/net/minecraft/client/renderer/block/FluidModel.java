package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record FluidModel(
    ChunkSectionLayer layer,
    Material.Baked stillMaterial,
    Material.Baked flowingMaterial,
    Material.@Nullable Baked overlayMaterial,
    net.neoforged.neoforge.client.fluid.@Nullable FluidTintSource fluidTintSource,
    net.neoforged.neoforge.client.fluid.@Nullable CustomFluidRenderer customRenderer
) {
    public FluidModel(ChunkSectionLayer layer, Material.Baked stillMaterial, Material.Baked flowingMaterial, Material.@Nullable Baked overlayMaterial, net.neoforged.neoforge.client.fluid.@Nullable FluidTintSource fluidTintSource) {
        this(layer, stillMaterial, flowingMaterial, overlayMaterial, fluidTintSource, null);
    }

    /// @deprecated Neo: use [#FluidModel(ChunkSectionLayer, Material.Baked, Material.Baked, Material.Baked, net.neoforged.neoforge.client.fluid.FluidTintSource)] instead
    @Deprecated
    public FluidModel(ChunkSectionLayer layer, Material.Baked stillMaterial, Material.Baked flowingMaterial, Material.@Nullable Baked overlayMaterial, @Nullable BlockTintSource tintSource) {
        this(layer, stillMaterial, flowingMaterial, overlayMaterial, net.neoforged.neoforge.client.fluid.FluidTintSources.of(tintSource));
    }

    /// @deprecated Neo: use [#fluidTintSource()] instead
    @Nullable
    @Deprecated
    public BlockTintSource tintSource() {
        return this.fluidTintSource;
    }

    public record Unbaked(Material stillMaterial, Material flowingMaterial, @Nullable Material overlayMaterial, net.neoforged.neoforge.client.fluid.@Nullable FluidTintSource fluidTintSource, net.neoforged.neoforge.client.fluid.@Nullable CustomFluidRenderer customRenderer) {
        public Unbaked(Material stillMaterial, Material flowingMaterial, @Nullable Material overlayMaterial, net.neoforged.neoforge.client.fluid.@Nullable FluidTintSource fluidTintSource) {
            this(stillMaterial, flowingMaterial, overlayMaterial, fluidTintSource, null);
        }

        /// @deprecated Neo: use [#Unbaked(Material, Material, Material, net.neoforged.neoforge.client.fluid.FluidTintSource)] instead
        @Deprecated
        public Unbaked(Material stillMaterial, Material flowingMaterial, @Nullable Material overlayMaterial, @Nullable BlockTintSource tintSource) {
            this(stillMaterial, flowingMaterial, overlayMaterial, net.neoforged.neoforge.client.fluid.FluidTintSources.of(tintSource));
        }

        /// @deprecated Neo: use [#fluidTintSource()] instead
        @Nullable
        @Deprecated
        public BlockTintSource tintSource() {
            return this.fluidTintSource;
        }

        public FluidModel bake(MaterialBaker materials, ModelDebugName modelName) {
            Material.Baked stillMaterial = this.getAndValidateMaterial(this.stillMaterial, materials, "still", modelName);
            Material.Baked flowingMaterial = this.getAndValidateMaterial(this.flowingMaterial, materials, "flowing", modelName);
            Material.Baked overlayMaterial = this.overlayMaterial != null
                ? this.getAndValidateMaterial(this.overlayMaterial, materials, "overlay", modelName)
                : null;
            Transparency transparency = getTransparency(stillMaterial).or(getTransparency(flowingMaterial));
            if (overlayMaterial != null) {
                transparency = transparency.or(getTransparency(overlayMaterial));
            }

            return new FluidModel(ChunkSectionLayer.byTransparency(transparency), stillMaterial, flowingMaterial, overlayMaterial, this.fluidTintSource, this.customRenderer);
        }

        private Material.Baked getAndValidateMaterial(Material material, MaterialBaker materials, String textureName, ModelDebugName modelName) {
            Material.Baked baked = materials.get(material, modelName);
            return !baked.sprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS) ? materials.reportMissingReference(textureName, modelName) : baked;
        }

        private static Transparency getTransparency(Material.Baked material) {
            return material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().transparency();
        }
    }
}
