package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MissingItemModel implements ItemModel {
    private final ItemQuads itemQuads;
    private final Supplier<Vector3fc[]> extents;
    private final ModelRenderProperties properties;
    private final Matrix4fc transform;

    public MissingItemModel(List<BakedQuad> quads, ModelRenderProperties properties) {
        this(ItemQuads.split(quads), Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads)), properties, new Matrix4f());
    }

    private MissingItemModel(ItemQuads itemQuads, Supplier<Vector3fc[]> extents, ModelRenderProperties properties, Matrix4fc transform) {
        this.itemQuads = itemQuads;
        this.extents = extents;
        this.properties = properties;
        this.transform = transform;
    }

    @Override
    public void update(
        ItemStackRenderState output,
        ItemStack item,
        ItemModelResolver resolver,
        ItemDisplayContext displayContext,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        output.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        this.properties.applyToLayer(layer, displayContext);
        layer.setExtents(this.extents);
        layer.setLocalTransform(this.transform);
        layer.setQuads(this.itemQuads);
    }

    public MissingItemModel withTransform(Matrix4fc transform) {
        return transform.equals(this.transform) ? this : new MissingItemModel(this.itemQuads, this.extents, this.properties, transform);
    }
}
