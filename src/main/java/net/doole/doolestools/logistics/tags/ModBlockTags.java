package net.doole.doolestools.logistics.tags;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    public static final TagKey<Block> SCANNER_BLACKLIST = TagKey.create(Registries.BLOCK, DoolesTools.id("scanner_blacklist"));

    private ModBlockTags() {}
}
