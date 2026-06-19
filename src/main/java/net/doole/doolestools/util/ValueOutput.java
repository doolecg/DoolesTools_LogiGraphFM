package net.doole.doolestools.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public final class ValueOutput {
    private final CompoundTag tag;
    private final HolderLookup.Provider registries;

    public ValueOutput(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag;
        this.registries = registries;
    }

    public void putInt(String key, int value) { tag.putInt(key, value); }
    public void putLong(String key, long value) { tag.putLong(key, value); }
    public void putBoolean(String key, boolean value) { tag.putBoolean(key, value); }
    public void putString(String key, String value) { tag.putString(key, value); }

    public <T> void store(String key, Codec<T> codec, T value) {
        codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value)
                .result()
                .ifPresent(encoded -> tag.put(key, encoded));
    }

    public <T> void storeNullable(String key, Codec<T> codec, T value) {
        if (value != null) store(key, codec, value);
    }
}
