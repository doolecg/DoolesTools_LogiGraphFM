package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: player wants to remove a peer link from this computer. */
public record UnlinkComputerPayload(BlockPos pos, BlockPos peerPos) implements CustomPacketPayload {
    public static final Type<UnlinkComputerPayload> TYPE = new Type<>(DoolesTools.id("unlink_computer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlinkComputerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, UnlinkComputerPayload::pos,
                    BlockPos.STREAM_CODEC, UnlinkComputerPayload::peerPos,
                    UnlinkComputerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
