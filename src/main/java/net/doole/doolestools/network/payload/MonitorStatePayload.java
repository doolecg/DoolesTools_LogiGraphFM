package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.network.ModStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** S2C: mirrored state for a monitor screen. */
public record MonitorStatePayload(BlockPos pos,
                                  boolean linked,
                                  BlockPos computerPos,
                                  int mode,
                                  LogisticsGraphData graph,
                                  List<ScannedBlockData> scan) implements CustomPacketPayload {
    public static final Type<MonitorStatePayload> TYPE = new Type<>(DoolesTools.id("monitor_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MonitorStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MonitorStatePayload::pos,
                    ByteBufCodecs.BOOL, MonitorStatePayload::linked,
                    BlockPos.STREAM_CODEC, MonitorStatePayload::computerPos,
                    ByteBufCodecs.VAR_INT, MonitorStatePayload::mode,
                    ModStreamCodecs.GRAPH, MonitorStatePayload::graph,
                    ModStreamCodecs.SCAN_LIST, MonitorStatePayload::scan,
                    MonitorStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
