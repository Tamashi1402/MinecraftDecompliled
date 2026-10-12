package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EquipmentLayerRenderer {
    private static final int NO_LAYER_COLOR = 0;
    private final EquipmentAssetManager equipmentAssets;
    private final Function<EquipmentLayerRenderer.LayerTextureKey, Identifier> layerTextureLookup;
    private final Function<EquipmentLayerRenderer.TrimTextureKey, PalettedTextureManager.Handle> trimTextureLookup;

    public EquipmentLayerRenderer(EquipmentAssetManager equipmentAssets, PalettedTextureManager palettedTextures) {
        this.equipmentAssets = equipmentAssets;
        this.layerTextureLookup = Util.memoize(key -> key.layer.getTextureLocation(key.layerType));
        this.trimTextureLookup = Util.memoize(key -> key.getOrPrepareTexture(palettedTextures));
    }

    public <S> void renderLayers(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> equipmentAssetId,
        Model<? super S> model,
        S state,
        ItemStack itemStack,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        int outlineColor
    ) {
        this.renderLayers(layerType, equipmentAssetId, model, state, itemStack, poseStack, submitNodeCollector, lightCoords, null, outlineColor, 1);
    }

    public <S> void renderLayers(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> equipmentAssetId,
        Model<? super S> model,
        S state,
        ItemStack itemStack,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        @Nullable Identifier playerTextureOverride,
        int outlineColor,
        int order
    ) {
        net.neoforged.neoforge.client.extensions.common.IClientItemExtensions extensions = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(itemStack);
        model = extensions.getGenericArmorModel(itemStack, layerType, model);
        EquipmentClientInfo equipmentInfo = this.equipmentAssets.get(equipmentAssetId);
        List<EquipmentClientInfo.Layer> layers = equipmentInfo.getLayers(layerType);
        if (!layers.isEmpty()) {
            int dyeColor = extensions.getDefaultDyeColor(itemStack);
            boolean hasFoil = itemStack.hasFoil();
            ArmorTrim trim = itemStack.get(DataComponents.TRIM);
            boolean hasTrim = trim != null && layerType != EquipmentClientInfo.LayerType.HUMANOID_BABY;
            boolean renderShaderGlint = hasFoil && !hasTrim;
            int nextOrder = order;

            int idx = 0;
            for (EquipmentClientInfo.Layer layer : layers) {
                int color = extensions.getArmorLayerTintColor(itemStack, layer, idx, dyeColor);
                if (color != 0) {
                    Identifier layerTexture = layer.usePlayerTexture() && playerTextureOverride != null
                        ? playerTextureOverride
                        : this.layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(layerType, layer));
                    layerTexture = net.neoforged.neoforge.client.ClientHooks.getArmorTexture(itemStack, layerType, layer, layerTexture);
                    RenderType renderType = renderShaderGlint ? RenderTypes.armorCutoutNoCullGlint(layerTexture) : RenderTypes.armorCutoutNoCull(layerTexture);
                    submitNodeCollector.order(nextOrder++)
                        .submitModel(model, state, poseStack, renderType, lightCoords, OverlayTexture.NO_OVERLAY, color, null, outlineColor);
                    renderShaderGlint = false;
                }
                idx++;
            }

            if (hasTrim) {
                PalettedTextureManager.Handle textureHandle = this.trimTextureLookup
                    .apply(new EquipmentLayerRenderer.TrimTextureKey(trim, layerType, equipmentInfo));
                RenderType renderType = RenderTypes.armorTrim(textureHandle.textureLocation(), trim.pattern().value().decal());
                submitNodeCollector.order(nextOrder++)
                    .submitModel(model, state, poseStack, renderType, lightCoords, OverlayTexture.NO_OVERLAY, -1, textureHandle, outlineColor);
                if (hasFoil) {
                    submitNodeCollector.order(nextOrder++)
                        .submitModel(model, state, poseStack, RenderTypes.trimmedArmorGlint(), lightCoords, OverlayTexture.NO_OVERLAY, -1, null, 0);
                }
            }
        }
    }

    public static int getColorForLayer(EquipmentClientInfo.Layer layer, int dyeColor) {
        Optional<EquipmentClientInfo.Dyeable> dyeable = layer.dyeable();
        if (dyeable.isPresent()) {
            int colorWhenUndyed = dyeable.get().colorWhenUndyed().map(ARGB::opaque).orElse(0);
            return dyeColor != 0 ? dyeColor : colorWhenUndyed;
        } else {
            return -1;
        }
    }

    private record LayerTextureKey(EquipmentClientInfo.LayerType layerType, EquipmentClientInfo.Layer layer) {
    }

    private record TrimTextureKey(ArmorTrim trim, EquipmentClientInfo.LayerType layerType, EquipmentClientInfo equipmentInfo) {
        private PalettedTextureManager.Handle getOrPrepareTexture(PalettedTextureManager palettedTextures) {
            Identifier textureId = this.trim.pattern().value().assetId();
            Identifier paletteId = this.trim.material().value().paletteId();

            for (EquipmentClientInfo.TrimOverride override : this.equipmentInfo.trimOverrides()) {
                if (override.predicate().matches(this.trim)) {
                    textureId = override.textureId().orElse(textureId);
                    paletteId = override.paletteId().orElse(null);
                    break;
                }
            }

            Identifier baseTexture = textureId.withPath(path -> this.layerType.trimAssetPrefix() + "/" + path);
            return paletteId == null ? createTextureWithNoPalette(baseTexture) : palettedTextures.getOrPrepare(baseTexture, paletteId);
        }

        private static PalettedTextureManager.Handle createTextureWithNoPalette(Identifier texture) {
            final Identifier textureLocation = texture.withPath(path -> "textures/" + path + ".png");
            return new PalettedTextureManager.Handle() {
                @Override
                public Identifier textureLocation() {
                    return textureLocation;
                }

                @Override
                public float getU(float offset) {
                    return offset;
                }

                @Override
                public float getV(float offset) {
                    return offset;
                }
            };
        }
    }
}
