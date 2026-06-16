package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: monitor screen requests the mirrored state from its linked computer. */
public record RequestMonitorSyncPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RequestMonitorSyncPayload> TYPE = new Type<>(DoolesTools.id("request_monitor_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestMonitorSyncPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RequestMonitorSyncPayload::pos, RequestMonitorSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
