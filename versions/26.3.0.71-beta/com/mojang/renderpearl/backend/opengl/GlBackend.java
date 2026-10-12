package com.mojang.renderpearl.backend.opengl;

import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import java.util.Objects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.SharedLibrary;

@OnlyIn(Dist.CLIENT)
public class GlBackend implements GpuBackend {
    private static final int VERSION_MAJOR = 3;
    private static final int VERSION_MINOR = 3;
    private boolean libraryLoaded;
    private @Nullable BackendCreationException libraryLoadFailure;

    @Override
    public String getName() {
        return "OpenGL";
    }

    @Override
    public void loadLibrary() throws BackendCreationException {
        if (!this.libraryLoaded) {
            if (this.libraryLoadFailure != null) {
                throw this.libraryLoadFailure;
            }

            // Neo: Ensure the ELS having already loaded GL does not prevent the GL backend from working
            if (!SDLVideo.SDL_GL_LoadLibrary(((SharedLibrary)GL.getFunctionProvider()).getPath()) && !Objects.equals(SDLError.SDL_GetError(), "OpenGL library already loaded")) {
                this.libraryLoadFailure = new BackendCreationException(
                    "OpenGL is not supported: " + Objects.requireNonNullElse(SDLError.SDL_GetError(), "<no error>"),
                    BackendCreationException.Reason.OPENGL_MISSING
                );
                throw this.libraryLoadFailure;
            }

            if (GL.getFunctionProvider().getFunctionAddress("glGetError") != SDLVideo.SDL_GL_GetProcAddress("glGetError")) {
                this.libraryLoadFailure = new BackendCreationException("glGetError mismatch", BackendCreationException.Reason.OPENGL_MISSING);
                SDLVideo.SDL_GL_UnloadLibrary();
                throw this.libraryLoadFailure;
            }

            this.libraryLoaded = true;
        }
    }

    @Override
    public void unloadLibrary() {
        if (this.libraryLoaded) {
            SDLVideo.SDL_GL_UnloadLibrary();
            this.libraryLoaded = false;
        }
    }

    @Override
    public long createWindow(@Nullable String title, int width, int height, long flags) {
        SDLVideo.SDL_GL_SetAttribute(17, 3);
        SDLVideo.SDL_GL_SetAttribute(18, 3);
        SDLVideo.SDL_GL_SetAttribute(20, 1);
        SDLVideo.SDL_GL_SetAttribute(19, 2);
        SDLVideo.SDL_GL_SetAttribute(22, 1);
        return SDLVideo.SDL_CreateWindow(title, width, height, 2L | flags);
    }

    @Override
    public GpuDevice createDevice(GpuDebugOptions debugOptions) throws BackendCreationException {
        return new FrontendGpuDevice(new GlDevice(this, debugOptions));
    }
}
