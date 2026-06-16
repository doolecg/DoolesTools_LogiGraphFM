package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** S2C: the block labels near the player, as parallel position/text lists. */
public record NearbyLabelsPayload(List<BlockPos> positions, List<String> labels) implements CustomPacketPayload {
    public static final Type<NearbyLabelsPayload> TYPE = new Type<>(DoolesTools.id("nearby_labels"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyLabelsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), NearbyLabelsPayload::positions,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), NearbyLabelsPayload::labels,
                    NearbyLabelsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
