package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ScreenEffectRenderer {
    private static final Identifier UNDERWATER_LOCATION = Identifier.withDefaultNamespace("textures/misc/underwater.png");
    private final GameRenderer gameRenderer;
    private final SpriteGetter sprites;

    public ScreenEffectRenderer(GameRenderer gameRenderer, SpriteGetter sprites) {
        this.gameRenderer = gameRenderer;
        this.sprites = sprites;
    }

    public void submit(
        float partialTicks, SubmitNodeCollector submitNodeCollector, PlayerRenderState playerRenderState, CameraRenderState cameraRenderState, boolean hideGui
    ) {
        PoseStack poseStack = new PoseStack();
        AvatarRenderState avatarRenderState = playerRenderState.avatarRenderState;
        if (!cameraRenderState.entityRenderState.isSleeping && playerRenderState.hasPlayer && avatarRenderState != null) {
            if (playerRenderState.customBlockOverlayRenderer == null || !playerRenderState.customBlockOverlayRenderer.submit(playerRenderState, cameraRenderState, submitNodeCollector, poseStack, this.sprites, partialTicks, hideGui))
            if (playerRenderState.blockOverlay != null) {
                submitBlockSprite(
                    playerRenderState.blockOverlay.atlasLocation(),
                    playerRenderState.blockOverlay.u0(),
                    playerRenderState.blockOverlay.v0(),
                    playerRenderState.blockOverlay.u1(),
                    playerRenderState.blockOverlay.v1(),
                    poseStack,
                    submitNodeCollector,
                    -15132391
                );
            }

            if (cameraRenderState.isFirstPerson && !avatarRenderState.isSpectator) {
                if (playerRenderState.customFluidOverlayRenderer == null || !playerRenderState.customFluidOverlayRenderer.submit(playerRenderState, cameraRenderState, submitNodeCollector, poseStack, this.sprites, partialTicks, hideGui))
                if (playerRenderState.waterOverlay != null) {
                    submitWater(playerRenderState.waterOverlay, poseStack, submitNodeCollector);
                }

                if (playerRenderState.isOnFire) {
                    TextureAtlasSprite fireSprite = this.sprites.get(ModelBakery.FIRE_1);
                    if (playerRenderState.customFireOverlayRenderer == null || !playerRenderState.customFireOverlayRenderer.submit(playerRenderState, cameraRenderState, submitNodeCollector, poseStack, this.sprites, partialTicks, hideGui))
                    submitFire(poseStack, submitNodeCollector, fireSprite);
                }
            }
        }

        if (!hideGui) {
            this.renderItemActivationAnimation(playerRenderState, poseStack, partialTicks, submitNodeCollector);
        }
    }

    private void renderItemActivationAnimation(
        PlayerRenderState playerRenderState, PoseStack poseStack, float partialTicks, SubmitNodeCollector submitNodeCollector
    ) {
        PlayerRenderState.ItemActivationRenderState itemActivation = playerRenderState.itemActivation;
        if (itemActivation != null) {
            int tick = 40 - itemActivation.ticks;
            float scale = (tick + partialTicks) / 40.0F;
            float ts = scale * scale;
            float tc = scale * ts;
            float smoothScale = 10.25F * tc * ts - 24.95F * ts * ts + 25.5F * tc - 13.8F * ts + 4.0F * scale;
            float piScale = smoothScale * (float) Math.PI;
            WindowRenderState windowState = this.gameRenderer.gameRenderState().windowRenderState;
            float aspectRatio = (float)windowState.width / windowState.height;
            float offX = itemActivation.offX * 0.3F * aspectRatio;
            float offY = itemActivation.offY * 0.3F;
            poseStack.pushPose();
            poseStack.translate(offX * Mth.abs(Mth.sin(piScale * 2.0F)), offY * Mth.abs(Mth.sin(piScale * 2.0F)), -10.0F + 9.0F * Mth.sin(piScale));
            float size = 0.8F;
            poseStack.scale(0.8F, 0.8F, 0.8F);
            poseStack.rotateDegrees(Axis.YP, 900.0F * Mth.abs(Mth.sin(piScale)));
            poseStack.rotateDegrees(Axis.XP, 6.0F * Mth.cos(scale * 8.0F));
            poseStack.rotateDegrees(Axis.ZP, 6.0F * Mth.cos(scale * 8.0F));
            this.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
            itemActivation.itemState.submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private static void submitBlockSprite(
        Identifier atlasLocation, float u0, float v0, float u1, float v1, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int color
    ) {
        submitNodeCollector.submitCustomGeometry(
            poseStack,
            RenderTypes.blockScreenEffect(atlasLocation),
            (pose, builder) -> buildQuad(builder, pose.pose(), -1.0F, -1.0F, 1.0F, 1.0F, -0.5F, u1, v1, u0, v0, color)
        );
    }

    private static void submitWater(PlayerRenderState.WaterOverlay waterOverlay, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        submitFluid(waterOverlay, poseStack, submitNodeCollector, UNDERWATER_LOCATION);
    }

    public static void submitFluid(PlayerRenderState.WaterOverlay waterOverlay, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Identifier texture) {
        submitNodeCollector.submitCustomGeometry(
            poseStack,
            RenderTypes.blockScreenEffect(texture),
            (pose, builder) -> {
                float uvSize = 4.0F;
                buildQuad(
                    builder,
                    pose.pose(),
                    -1.0F,
                    -1.0F,
                    1.0F,
                    1.0F,
                    -0.5F,
                    waterOverlay.uOffset() + 4.0F,
                    waterOverlay.vOffset() + 4.0F,
                    waterOverlay.uOffset(),
                    waterOverlay.vOffset(),
                    waterOverlay.color()
                );
            }
        );
    }

    private static void submitFire(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite sprite) {
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.fireScreenEffect(sprite.atlasLocation()), (basePose, builder) -> {
            Matrix4f pose = new Matrix4f();
            pose.set(basePose.pose());
            pose.translate(0.24F, -0.3F, 0.0F);
            pose.rotateY((float) (-Math.PI / 18));
            buildFireQuad(sprite, builder, pose);
            pose.set(basePose.pose());
            pose.translate(-0.24F, -0.3F, 0.0F);
            pose.rotateY((float) (Math.PI / 18));
            buildFireQuad(sprite, builder, pose);
        });
    }

    private static void buildFireQuad(TextureAtlasSprite sprite, VertexConsumer builder, Matrix4f pose) {
        float size = 1.0F;
        buildSpriteQuad(builder, pose, sprite, -0.5F, -0.5F, 0.5F, 0.5F, -0.5F, -436207617);
    }

    private static void buildSpriteQuad(
        VertexConsumer builder, Matrix4f pose, TextureAtlasSprite sprite, float x0, float y0, float x1, float y1, float z, int color
    ) {
        buildQuad(builder, pose, x0, y0, x1, y1, z, sprite.getU1(), sprite.getV1(), sprite.getU0(), sprite.getV0(), color);
    }

    private static void buildQuad(
        VertexConsumer builder, Matrix4f pose, float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1, int color
    ) {
        builder.addVertex(pose, x0, y0, z).setUv(u0, v0).setColor(color);
        builder.addVertex(pose, x1, y0, z).setUv(u1, v0).setColor(color);
        builder.addVertex(pose, x1, y1, z).setUv(u1, v1).setColor(color);
        builder.addVertex(pose, x0, y1, z).setUv(u0, v1).setColor(color);
    }
}
