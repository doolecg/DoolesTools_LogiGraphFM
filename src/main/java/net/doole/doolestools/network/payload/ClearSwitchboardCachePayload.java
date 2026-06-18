package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClearSwitchboardCachePayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ClearSwitchboardCachePayload> TYPE = new Type<>(DoolesTools.id("clear_switchboard_cache"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearSwitchboardCachePayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, ClearSwitchboardCachePayload::pos, ClearSwitchboardCachePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
