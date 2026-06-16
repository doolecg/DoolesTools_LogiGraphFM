package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.NetworkPowerData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.network.ModStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** S2C: full computer state for the open editor screen. */
public record ComputerStatePayload(BlockPos pos,
                                     List<ScannedBlockData> scan,
                                     LogisticsGraphData graph,
                                     long lastScanTime,
                                     NetworkPowerData power,
                                      List<String> activeRouteIds,
                                      String networkId,
                                      String networkName,
                                      String accessMode,
                                      List<String> editorWhitelist,
                                      boolean canEdit,
                                      List<Integer> powerSupplyHistory,
                                      List<Integer> powerDemandHistory) implements CustomPacketPayload {
    public static final Type<ComputerStatePayload> TYPE = new Type<>(DoolesTools.id("computer_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerStatePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ComputerStatePayload decode(RegistryFriendlyByteBuf buf) {
                    return new ComputerStatePayload(
                            BlockPos.STREAM_CODEC.decode(buf),
                            ModStreamCodecs.SCAN_LIST.decode(buf),
                            ModStreamCodecs.GRAPH.decode(buf),
                            ByteBufCodecs.VAR_LONG.decode(buf),
                            ModStreamCodecs.POWER.decode(buf),
                            ModStreamCodecs.STRING_LIST.decode(buf),
                            ByteBufCodecs.stringUtf8(128).decode(buf),
                            ByteBufCodecs.stringUtf8(192).decode(buf),
                            ByteBufCodecs.stringUtf8(32).decode(buf),
                            ModStreamCodecs.STRING_LIST.decode(buf),
                            ByteBufCodecs.BOOL.decode(buf),
                            ModStreamCodecs.INT_LIST.decode(buf),
                            ModStreamCodecs.INT_LIST.decode(buf));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, ComputerStatePayload payload) {
                    BlockPos.STREAM_CODEC.encode(buf, payload.pos());
                    ModStreamCodecs.SCAN_LIST.encode(buf, payload.scan());
                    ModStreamCodecs.GRAPH.encode(buf, payload.graph());
                    ByteBufCodecs.VAR_LONG.encode(buf, payload.lastScanTime());
                    ModStreamCodecs.POWER.encode(buf, payload.power());
                    ModStreamCodecs.STRING_LIST.encode(buf, payload.activeRouteIds());
                    ByteBufCodecs.stringUtf8(128).encode(buf, payload.networkId());
                    ByteBufCodecs.stringUtf8(192).encode(buf, payload.networkName());
                    ByteBufCodecs.stringUtf8(32).encode(buf, payload.accessMode());
                    ModStreamCodecs.STRING_LIST.encode(buf, payload.editorWhitelist());
                    ByteBufCodecs.BOOL.encode(buf, payload.canEdit());
                    ModStreamCodecs.INT_LIST.encode(buf, payload.powerSupplyHistory());
                    ModStreamCodecs.INT_LIST.encode(buf, payload.powerDemandHistory());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
