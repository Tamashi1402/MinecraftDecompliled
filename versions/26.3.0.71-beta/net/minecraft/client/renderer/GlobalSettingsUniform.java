package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class GlobalSettingsUniform implements AutoCloseable {
    public static final int UBO_SIZE = new Std140SizeCalculator().putIVec3().putFloat().putVec3().putFloat().putVec2().putInt().putInt().get();
    private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Global Settings UBO", 136, UBO_SIZE);

    public void update(int width, int height, double glintAlpha, long gameTime, float worldPartialTicks, int menuBlurRadius, Vec3 cameraPos, boolean useRgss) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int cameraX = Mth.floor(cameraPos.x);
            int cameraY = Mth.floor(cameraPos.y);
            int cameraZ = Mth.floor(cameraPos.z);
            ByteBuffer data = Std140Builder.onStack(stack, UBO_SIZE)
                .putIVec3(cameraX, cameraY, cameraZ)
                .putFloat((float)glintAlpha)
                .putVec3((float)(cameraX - cameraPos.x), (float)(cameraY - cameraPos.y), (float)(cameraZ - cameraPos.z))
                .putFloat(((float)(gameTime % 24000L) + worldPartialTicks) / 24000.0F)
                .putVec2(width, height)
                .putInt(menuBlurRadius)
                .putInt(useRgss ? 1 : 0)
                .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), data);
        }

        RenderSystem.setGlobalSettingsUniform(this.buffer);
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}
