package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.tags.DialogTags;

public class DialogTagsProvider extends TagsProvider<Dialog> {
    /// @deprecated Neo: Use the [DialogTagsProvider(PackOutput, CompletableFuture, String)] mod id variant
    @Deprecated
    public DialogTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.DIALOG, lookupProvider);
    }

    public DialogTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId) {
        super(output, Registries.DIALOG, lookupProvider, modId);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(DialogTags.PAUSE_SCREEN_ADDITIONS);
        this.tag(DialogTags.QUICK_ACTIONS);
    }
}
