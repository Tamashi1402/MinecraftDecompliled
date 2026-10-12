package net.minecraft.client.gui.components.debug;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryPostEffects implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        Minecraft minecraft = Minecraft.getInstance();
        List<Identifier> effectIds = minecraft.gameRenderer.getAppliedPostEffects();
        if (!effectIds.isEmpty()) {
            displayer.addLine("Post: " + effectIds.stream().map(Identifier::toString).collect(Collectors.joining(", ")));
        }
    }
}
