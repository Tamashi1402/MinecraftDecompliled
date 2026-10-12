package com.mojang.blaze3d.platform.cursor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.sdl.SDLMouse;

@OnlyIn(Dist.CLIENT)
public class CursorType {
    public static final CursorType DEFAULT = new CursorType("default", 0L);
    private final String name;
    private final long handle;

    private CursorType(String name, long handle) {
        this.name = name;
        this.handle = handle;
    }

    public void select() {
        long cursor = this.handle == 0L ? SDLMouse.SDL_GetDefaultCursor() : this.handle;
        SDLMouse.SDL_SetCursor(cursor);
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static CursorType createStandardCursor(int shape, String name, CursorType fallback) {
        long handle = SDLMouse.SDL_CreateSystemCursor(shape);
        return handle == 0L ? fallback : new CursorType(name, handle);
    }
}
