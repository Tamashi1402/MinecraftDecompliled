package com.mojang.blaze3d.platform;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.sdl.SDL_Rect.Buffer;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class TextInputManager {
    private final Window window;
    private boolean textInputEnabled;
    private @Nullable Object owner;
    private boolean hasTextInputArea;
    private int areaX;
    private int areaY;
    private int areaWidth;
    private int areaHeight;

    public TextInputManager(Window window) {
        this.window = window;
    }

    public void setTextInputArea(int x0, int y0, int x1, int y1) {
        double windowScale = (double)this.window.getGuiScale() / this.window.getPixelDensity();
        int x = (int)Math.round(x0 * windowScale);
        int y = (int)Math.round(y0 * windowScale);
        int width = Math.max(1, (int)Math.round((x1 - x0) * windowScale));
        int height = Math.max(1, (int)Math.round((y1 - y0) * windowScale));
        if (!this.hasTextInputArea || x != this.areaX || y != this.areaY || width != this.areaWidth || height != this.areaHeight) {
            this.areaX = x;
            this.areaY = y;
            this.areaWidth = width;
            this.areaHeight = height;
            this.hasTextInputArea = true;
            this.applyTextInputArea();
        }
    }

    private void applyTextInputArea() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Buffer rect = SDL_Rect.malloc(1, stack).x(this.areaX).y(this.areaY).w(this.areaWidth).h(this.areaHeight);
            SDLKeyboard.SDL_SetTextInputArea(this.window.handle(), rect, -1);
        }
    }

    public void startTextInput(Object owner) {
        this.owner = owner;
        if (!this.textInputEnabled) {
            if (this.hasTextInputArea) {
                this.applyTextInputArea();
            }

            this.textInputEnabled = true;
            SDLKeyboard.SDL_StartTextInput(this.window.handle());
            SDLKeyboard.SDL_ClearComposition(this.window.handle());
        }
    }

    public void stopTextInput(Object owner) {
        if (this.owner == owner) {
            this.stopTextInput();
        }
    }

    public void stopTextInput() {
        this.owner = null;
        if (this.textInputEnabled) {
            this.textInputEnabled = false;
            SDLKeyboard.SDL_StopTextInput(this.window.handle());
        }
    }

    public void onTextInputFocusChange(Object owner, boolean focused) {
        if (focused) {
            this.startTextInput(owner);
        } else {
            this.stopTextInput(owner);
        }
    }
}
