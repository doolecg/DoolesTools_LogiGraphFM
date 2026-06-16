package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.network.ModStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: persist the edited flowgraph back to the computer block entity. */
public record SaveGraphPayload(BlockPos pos, LogisticsGraphData graph) implements CustomPacketPayload {
    public static final Type<SaveGraphPayload> TYPE = new Type<>(DoolesTools.id("save_graph"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveGraphPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SaveGraphPayload::pos,
                    ModStreamCodecs.GRAPH, SaveGraphPayload::graph,
                    SaveGraphPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
