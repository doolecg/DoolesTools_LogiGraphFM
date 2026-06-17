package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestSwitchboardStatePayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RequestSwitchboardStatePayload> TYPE = new Type<>(DoolesTools.id("request_switchboard_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSwitchboardStatePayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RequestSwitchboardStatePayload::pos, RequestSwitchboardStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
