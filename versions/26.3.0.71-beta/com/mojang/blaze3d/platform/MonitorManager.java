package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.nio.IntBuffer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLStdinc;
import org.lwjgl.sdl.SDLVideo;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class MonitorManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Int2ObjectMap<Monitor> monitors = new Int2ObjectOpenHashMap<>();

    public MonitorManager() {
        IntBuffer displays = SDLVideo.SDL_GetDisplays();
        if (displays != null) {
            try {
                for (int i = 0; i < displays.limit(); i++) {
                    this.addDisplay(displays.get(i));
                }
            } finally {
                SDLStdinc.SDL_free(displays);
            }
        }
    }

    public void onDisplayConnected(int id) {
        RenderSystem.assertOnRenderThread();
        Monitor monitor = this.addDisplay(id);
        if (monitor != null) {
            LOGGER.debug("Monitor {} connected. Current monitors: {}", monitor, this.monitors);
        }
    }

    public void onDisplayDisconnected(int id) {
        RenderSystem.assertOnRenderThread();
        Monitor monitor = this.monitors.remove(id);
        LOGGER.debug("Monitor {} disconnected. Current monitors: {}", monitor, this.monitors);
    }

    public void onDisplayModeChanged(int id) {
        RenderSystem.assertOnRenderThread();
        Monitor monitor = this.addDisplay(id);
        if (monitor != null) {
            LOGGER.debug("Monitor {} mode changed. Current monitors: {}", monitor, this.monitors);
        }
    }

    private @Nullable Monitor addDisplay(int id) {
        Monitor monitor = Monitor.tryCreate(id);
        if (monitor != null) {
            this.monitors.put(id, monitor);
        }

        return monitor;
    }

    public @Nullable Monitor getMonitor(int id) {
        return this.monitors.get(id);
    }

    public @Nullable Monitor findBestMonitor(Window window) {
        return this.getMonitor(SDLVideo.SDL_GetDisplayForWindow(window.handle()));
    }
}
