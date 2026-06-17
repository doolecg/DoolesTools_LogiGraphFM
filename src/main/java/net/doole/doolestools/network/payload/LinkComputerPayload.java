package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S: player wants to link this computer to a peer by network ID.
 * The target network ID is resolved server-side to a block position.
 */
public record LinkComputerPayload(BlockPos pos, String targetNetworkId) implements CustomPacketPayload {
    public static final Type<LinkComputerPayload> TYPE = new Type<>(DoolesTools.id("link_computer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkComputerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, LinkComputerPayload::pos,
                    ByteBufCodecs.stringUtf8(64), LinkComputerPayload::targetNetworkId,
                    LinkComputerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
