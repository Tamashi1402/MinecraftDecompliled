package com.mojang.renderpearl.backend.opengl;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import java.util.Objects;
import java.util.Optional;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GlRenderPipeline implements BackendRenderPipeline {
    private final GlDevice device;
    private final GlProgram program;
    private final VertexArray vertexArray;
    private final int primitiveTopology;
    private final boolean cull;
    private final int polygonMode;
    private final boolean depthEnabled;
    private final int depthFunc;
    private final boolean writeDepth;
    private final float depthBiasConstant;
    private final float depthBiasScaleFactor;
    private final @ColorTargetState.WriteMask int[] writeMasks;
    private final boolean[] blendEnabled;
    private final int srcRgb;
    private final int dstRgb;
    private final int modeRgb;
    private final int srcAlpha;
    private final int dstAlpha;
    private final int modeAlpha;
    private final boolean hasStencilTest;
    private final boolean hasPerFaceStencilTest;
    private final int stencilReferenceValue;
    private final int stencilReadMask;
    private final int stencilWriteMask;
    private final int stencilFrontCompare;
    private final int stencilFrontFail;
    private final int stencilFrontDepthFail;
    private final int stencilFrontPass;
    private final int stencilBackCompare;
    private final int stencilBackFail;
    private final int stencilBackDepthFail;
    private final int stencilBackPass;
    private boolean closed = false;

    GlRenderPipeline(GlDevice device, BackendRenderPipeline.CreateInfo createInfo, GlProgram program, VertexArray vertexArray) {
        this.device = device;
        this.program = program;
        this.vertexArray = vertexArray;
        this.primitiveTopology = GlConst.toGl(createInfo.primitiveTopology());
        this.cull = createInfo.cull();
        this.polygonMode = GlConst.toGl(createInfo.polygonMode());
        this.depthEnabled = createInfo.depthStencilState() != null;
        if (this.depthEnabled) {
            this.depthFunc = GlConst.toGl(createInfo.depthStencilState().depthTest());
            this.writeDepth = createInfo.depthStencilState().writeDepth();
            this.depthBiasConstant = createInfo.depthStencilState().depthBiasConstant();
            this.depthBiasScaleFactor = createInfo.depthStencilState().depthBiasScaleFactor();
        } else {
            this.depthFunc = 1280;
            this.writeDepth = false;
            this.depthBiasConstant = 0.0F;
            this.depthBiasScaleFactor = 0.0F;
        }

        this.writeMasks = new int[createInfo.colorTargetStates().size()];
        this.blendEnabled = new boolean[createInfo.colorTargetStates().size()];

        for (int i = 0; i < createInfo.colorTargetStates().size(); i++) {
            ColorTargetState state = createInfo.colorTargetStates().get(i);
            if (state != null) {
                this.writeMasks[i] = state.writeMask() & 15;
                this.blendEnabled[i] = state.blendFunction().isPresent();
            } else {
                this.writeMasks[i] = -1;
                this.blendEnabled[i] = false;
            }
        }

        Optional<BlendFunction> representativeBlendState = createInfo.colorTargetStates()
            .stream()
            .filter(Objects::nonNull)
            .map(ColorTargetState::blendFunction)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
        if (representativeBlendState.isPresent()) {
            BlendFunction blendFunction = representativeBlendState.get();
            this.srcRgb = GlConst.toGl(blendFunction.color().sourceFactor());
            this.dstRgb = GlConst.toGl(blendFunction.color().destFactor());
            this.modeRgb = GlConst.toGl(blendFunction.color().op());
            this.srcAlpha = GlConst.toGl(blendFunction.alpha().sourceFactor());
            this.dstAlpha = GlConst.toGl(blendFunction.alpha().destFactor());
            this.modeAlpha = GlConst.toGl(blendFunction.alpha().op());
        } else {
            this.srcRgb = 1280;
            this.dstRgb = 1280;
            this.modeRgb = 1280;
            this.srcAlpha = 1280;
            this.dstAlpha = 1280;
            this.modeAlpha = 1280;
        }

        var stencilTest = this.depthEnabled ? createInfo.depthStencilState().stencilTest() : null;
        if (stencilTest != null) {
            this.hasStencilTest = true;
            this.hasPerFaceStencilTest = !stencilTest.front().equals(stencilTest.back());
            this.stencilReferenceValue = stencilTest.referenceValue();
            this.stencilReadMask = stencilTest.readMask();
            this.stencilWriteMask = stencilTest.writeMask();
            this.stencilFrontCompare = GlConst.toGl(stencilTest.front().compare());
            this.stencilFrontFail = GlConst.toGl(stencilTest.front().fail());
            this.stencilFrontDepthFail = GlConst.toGl(stencilTest.front().depthFail());
            this.stencilFrontPass = GlConst.toGl(stencilTest.front().pass());
            this.stencilBackCompare = GlConst.toGl(stencilTest.back().compare());
            this.stencilBackFail = GlConst.toGl(stencilTest.back().fail());
            this.stencilBackDepthFail = GlConst.toGl(stencilTest.back().depthFail());
            this.stencilBackPass = GlConst.toGl(stencilTest.back().pass());
        } else {
            this.hasStencilTest = false;
            this.hasPerFaceStencilTest = false;
            this.stencilReferenceValue = 0;
            this.stencilReadMask = 0;
            this.stencilWriteMask = 0;
            this.stencilFrontCompare = 0;
            this.stencilFrontFail = 0;
            this.stencilFrontDepthFail = 0;
            this.stencilFrontPass = 0;
            this.stencilBackCompare = 0;
            this.stencilBackFail = 0;
            this.stencilBackDepthFail = 0;
            this.stencilBackPass = 0;
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.program.close();
            this.vertexArray.close();
            this.device.markAmdShaderCompilerAngry();
        }
    }

    public GlProgram program() {
        return this.program;
    }

    public VertexArray vertexArray() {
        return this.vertexArray;
    }

    public void bind() {
        GlStateManager._glUseProgram(this.program.getProgramId());
        if (this.depthEnabled) {
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(this.depthFunc);
            GlStateManager._depthMask(this.writeDepth);
            if (this.depthBiasConstant == 0.0F && this.depthBiasScaleFactor == 0.0F) {
                GlStateManager._disablePolygonOffset();
            } else {
                GlStateManager._polygonOffset(this.depthBiasScaleFactor, this.depthBiasConstant);
                GlStateManager._enablePolygonOffset();
            }
        } else {
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask(false);
            GlStateManager._disablePolygonOffset();
        }

        if (this.cull) {
            GlStateManager._enableCull();
        } else {
            GlStateManager._disableCull();
        }

        for (int i = 0; i < this.writeMasks.length; i++) {
            if (this.writeMasks[i] != -1) {
                GlStateManager._colorMask(i, this.writeMasks[i]);
            } else {
                GlStateManager._colorMask(i, 0);
            }

            if (this.blendEnabled[i]) {
                GlStateManager._enableBlend(i);
            } else {
                GlStateManager._disableBlend(i);
            }
        }

        if (this.srcRgb != 1280) {
            GlStateManager._blendFuncSeparate(this.srcRgb, this.dstRgb, this.srcAlpha, this.dstAlpha);
            GlStateManager._blendEquationSeparate(this.modeRgb, this.modeAlpha);
        }

        GlStateManager._polygonMode(1032, this.polygonMode);

        if (this.hasStencilTest) {
            GlStateManager._enableStencilTest();
            if (this.hasPerFaceStencilTest) {
                GlStateManager._stencilFuncFront(this.stencilFrontCompare, this.stencilReferenceValue, this.stencilReadMask);
                GlStateManager._stencilFuncBack(this.stencilBackCompare, this.stencilReferenceValue, this.stencilReadMask);
                GlStateManager._stencilOpFront(this.stencilFrontFail, this.stencilFrontDepthFail, this.stencilFrontPass);
                GlStateManager._stencilOpBack(this.stencilBackFail, this.stencilBackDepthFail, this.stencilBackPass);
            } else {
                GlStateManager._stencilFunc(this.stencilFrontCompare, this.stencilReferenceValue, this.stencilReadMask);
                GlStateManager._stencilOp(this.stencilFrontFail, this.stencilFrontDepthFail, this.stencilFrontPass);
            }
            GlStateManager._stencilMask(this.stencilWriteMask);
        } else {
            GlStateManager._disableStencilTest();
        }
    }

    public int primitiveTopology() {
        return this.primitiveTopology;
    }
}
