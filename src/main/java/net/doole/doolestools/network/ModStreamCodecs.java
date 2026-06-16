package net.doole.doolestools.network;

import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.NetworkPowerData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

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
}
