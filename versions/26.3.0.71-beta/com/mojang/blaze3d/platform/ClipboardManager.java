package com.mojang.blaze3d.platform;

import com.mojang.logging.LogUtils;
import net.minecraft.util.StringDecomposer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.sdl.SDLClipboard;
import org.lwjgl.sdl.SDLError;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClipboardManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public String getClipboard() {
        String clipboard = SDLClipboard.SDL_GetClipboardText();
        if (clipboard == null) {
            LOGGER.error("Failed to read clipboard: {}", SDLError.SDL_GetError());
            return "";
        } else {
            return StringDecomposer.filterBrokenSurrogates(clipboard);
        }
    }

    public void setClipboard(String clipboard) {
        if (!SDLClipboard.SDL_SetClipboardText(clipboard)) {
            LOGGER.error("Failed to set clipboard: {}", SDLError.SDL_GetError());
        }
    }
}
