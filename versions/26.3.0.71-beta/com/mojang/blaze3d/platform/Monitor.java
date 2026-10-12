package com.mojang.blaze3d.platform;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLStdinc;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record Monitor(String name, int id, List<VideoMode> videoModes, VideoMode currentMode, int x, int y, int w, int h) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    public static @Nullable Monitor tryCreate(int id) {
        String name = queryMonitorName(id);
        Builder<VideoMode> videoModes = ImmutableList.builder();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer modes = SDLVideo.SDL_GetFullscreenDisplayModes(id);
            if (modes == null) {
                LOGGER.warn("Failed to query video modes of monitor {}: {}", name, SDLError.SDL_GetError());
                return null;
            }

            try {
                for (int i = 0; i < modes.limit(); i++) {
                    VideoMode mode = new VideoMode(SDL_DisplayMode.create(modes.get(i)));
                    if (mode.getRedBits() >= 8 && mode.getGreenBits() >= 8 && mode.getBlueBits() >= 8) {
                        videoModes.add(mode);
                    }
                }
            } finally {
                SDLStdinc.SDL_free(modes);
            }

            SDL_Rect var14 = SDL_Rect.malloc(stack);
            if (!SDLVideo.SDL_GetDisplayBounds(id, var14)) {
                LOGGER.warn("Failed to query monitor bounds of {}: {}", name, SDLError.SDL_GetError());
                return null;
            } else {
                SDL_DisplayMode currentMode = SDLVideo.SDL_GetDesktopDisplayMode(id);
                if (currentMode == null) {
                    LOGGER.warn("Failed to query current desktop video mode of monitor {}: {}", name, SDLError.SDL_GetError());
                    return null;
                } else {
                    return new Monitor(name, id, videoModes.build(), new VideoMode(currentMode), var14.x(), var14.y(), var14.w(), var14.h());
                }
            }
        }
    }

    private static String queryMonitorName(int id) {
        String monitorName = Objects.requireNonNullElse(SDLVideo.SDL_GetDisplayName(id), "unknown");
        return monitorName + "[0x" + HEX_FORMAT.toHexDigits(id) + "]";
    }

    public VideoMode getPreferredVideoMode(Optional<VideoMode> expectedMode) {
        return expectedMode.filter(this.videoModes::contains).orElse(this.currentMode);
    }

    public int indexOfMode(VideoMode expectedMode) {
        return this.videoModes.indexOf(expectedMode);
    }

    public VideoMode mode(int mode) {
        return this.videoModes.get(mode);
    }

    public int modeCount() {
        return this.videoModes.size();
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s(%s at (%d,%d))", this.name, this.currentMode, this.x, this.y);
    }
}
