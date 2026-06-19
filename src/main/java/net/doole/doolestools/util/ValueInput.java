package net.doole.doolestools.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import java.util.Optional;

public final class ValueInput {
    private final CompoundTag tag;
    private final HolderLookup.Provider registries;

    public ValueInput(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag;
        this.registries = registries;
    }

    public int getIntOr(String key, int fallback) { return tag.contains(key) ? tag.getInt(key) : fallback; }
    public long getLongOr(String key, long fallback) { return tag.contains(key) ? tag.getLong(key) : fallback; }
    public boolean getBooleanOr(String key, boolean fallback) { return tag.contains(key) ? tag.getBoolean(key) : fallback; }
    public String getStringOr(String key, String fallback) { return tag.contains(key) ? tag.getString(key) : fallback; }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        if (!tag.contains(key)) return Optional.empty();
        return codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(key)).result();
    }
}
