package net.doole.doolestools.network;

import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.NetworkPowerData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reused stream codecs derived from the data-model {@link com.mojang.serialization.Codec}s. */
public final class ModStreamCodecs {
    private ModStreamCodecs() {}

    public static final StreamCodec<RegistryFriendlyByteBuf, LogisticsGraphData> GRAPH =
            ByteBufCodecs.fromCodecWithRegistries(LogisticsGraphData.CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, List<ScannedBlockData>> SCAN_LIST =
            ByteBufCodecs.fromCodecWithRegistries(ScannedBlockData.CODEC.listOf());

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkPowerData> POWER =
            ByteBufCodecs.fromCodecWithRegistries(NetworkPowerData.CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, List<String>> STRING_LIST =
            ByteBufCodecs.fromCodecWithRegistries(com.mojang.serialization.Codec.STRING.listOf());

    public static final StreamCodec<RegistryFriendlyByteBuf, List<Integer>> INT_LIST =
            ByteBufCodecs.fromCodecWithRegistries(com.mojang.serialization.Codec.INT.listOf());

    public static final StreamCodec<RegistryFriendlyByteBuf, Map<String, List<Integer>>> LINK_THROUGHPUT_MAP =
            new StreamCodec<>() {
                @Override
                public Map<String, List<Integer>> decode(RegistryFriendlyByteBuf buf) {
                    int size = ByteBufCodecs.VAR_INT.decode(buf);
                    Map<String, List<Integer>> map = new HashMap<>(size);
                    for (int i = 0; i < size; i++) {
                        String key = ByteBufCodecs.stringUtf8(256).decode(buf);
                        map.put(key, INT_LIST.decode(buf));
                    }
                    return map;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, Map<String, List<Integer>> map) {
                    ByteBufCodecs.VAR_INT.encode(buf, map.size());
                    for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
                        ByteBufCodecs.stringUtf8(256).encode(buf, entry.getKey());
                        INT_LIST.encode(buf, entry.getValue());
                    }
                }
            };

    private static final com.mojang.serialization.Codec<Short> SHORT_CODEC =
            com.mojang.serialization.Codec.INT.xmap(i -> (short) (int) i, s -> (int) s);

    public static final StreamCodec<RegistryFriendlyByteBuf, List<Short>> SHORT_LIST =
            ByteBufCodecs.fromCodecWithRegistries(SHORT_CODEC.listOf());
}
