package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.network.ModStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** S2C: updated list of linked peer computers for this computer, sent after link/unlink. */
public record LinkedComputersPayload(BlockPos pos, List<BlockPos> peerPositions, List<String> peerNetworkIds) implements CustomPacketPayload {
    public static final Type<LinkedComputersPayload> TYPE = new Type<>(DoolesTools.id("linked_computers"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<BlockPos>> POS_LIST =
            ByteBufCodecs.fromCodecWithRegistries(BlockPos.CODEC.listOf());

    private static final StreamCodec<RegistryFriendlyByteBuf, List<String>> STR_LIST =
            ModStreamCodecs.STRING_LIST;

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkedComputersPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, LinkedComputersPayload::pos,
                    POS_LIST, LinkedComputersPayload::peerPositions,
                    STR_LIST, LinkedComputersPayload::peerNetworkIds,
                    LinkedComputersPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
