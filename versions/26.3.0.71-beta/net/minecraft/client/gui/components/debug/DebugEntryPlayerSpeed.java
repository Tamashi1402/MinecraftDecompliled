package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryPlayerSpeed implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        if (Minecraft.getInstance().getCameraEntity() != null) {
            displayer.addToGroup(
                DebugEntryPosition.GROUP,
                String.format(Locale.ROOT, "Speed: %.3f blocks/tick", Minecraft.getInstance().getCameraEntity().getKnownSpeed().length())
            );
        }
    }
}
