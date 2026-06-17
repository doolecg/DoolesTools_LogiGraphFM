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
import java.util.Map;

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
                                      List<Integer> powerDemandHistory,
                                      List<Short> supply30m,
                                      List<Short> demand30m,
                                      List<Short> supply1h,
                                      List<Short> demand1h,
                                      List<Short> supply12h,
                                      List<Short> demand12h,
                                       List<Short> supply1d,
                                       List<Short> demand1d,
                                       List<Short> supplyAllTime,
                                       List<Short> demandAllTime,
                                       Map<String, List<Integer>> linkThroughput) implements CustomPacketPayload {
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
                            ModStreamCodecs.INT_LIST.decode(buf),
                            ModStreamCodecs.SHORT_LIST.decode(buf),
                            ModStreamCodecs.SHORT_LIST.decode(buf),
                            ModStreamCodecs.SHORT_LIST.decode(buf),
                            ModStreamCodecs.SHORT_LIST.decode(buf),
                            ModStreamCodecs.SHORT_LIST.decode(buf),
                            ModStreamCodecs.SHORT_LIST.decode(buf),
                            ModStreamCodecs.SHORT_LIST.decode(buf),
                            ModStreamCodecs.SHORT_LIST.decode(buf),
                            ModStreamCodecs.SHORT_LIST.decode(buf),
                            ModStreamCodecs.SHORT_LIST.decode(buf),
                            ModStreamCodecs.LINK_THROUGHPUT_MAP.decode(buf));
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
                    ModStreamCodecs.SHORT_LIST.encode(buf, payload.supply30m());
                    ModStreamCodecs.SHORT_LIST.encode(buf, payload.demand30m());
                    ModStreamCodecs.SHORT_LIST.encode(buf, payload.supply1h());
                    ModStreamCodecs.SHORT_LIST.encode(buf, payload.demand1h());
                    ModStreamCodecs.SHORT_LIST.encode(buf, payload.supply12h());
                    ModStreamCodecs.SHORT_LIST.encode(buf, payload.demand12h());
                    ModStreamCodecs.SHORT_LIST.encode(buf, payload.supply1d());
                    ModStreamCodecs.SHORT_LIST.encode(buf, payload.demand1d());
                    ModStreamCodecs.SHORT_LIST.encode(buf, payload.supplyAllTime());
                    ModStreamCodecs.SHORT_LIST.encode(buf, payload.demandAllTime());
                    ModStreamCodecs.LINK_THROUGHPUT_MAP.encode(buf, payload.linkThroughput());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
