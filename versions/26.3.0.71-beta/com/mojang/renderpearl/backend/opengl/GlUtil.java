package com.mojang.renderpearl.backend.opengl;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlUtil {
    public static int selectBufferBindTarget(@GpuBuffer.Usage int usage) {
        if ((usage & 32) != 0) {
            return 34962;
        } else if ((usage & 64) != 0) {
            return 34963;
        } else if ((usage & 128) != 0) {
            return 35345;
        } else {
            return (usage & 512) != 0 ? 36671 : 36663;
        }
    }
}
