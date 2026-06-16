package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: client asks the server for the computer's current scan + graph (sent on screen open). */
public record RequestComputerSyncPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RequestComputerSyncPayload> TYPE =
            new Type<>(DoolesTools.id("request_computer_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestComputerSyncPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RequestComputerSyncPayload::pos, RequestComputerSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
