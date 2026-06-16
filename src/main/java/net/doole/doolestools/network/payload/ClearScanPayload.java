package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: player pressed "Clear Scan". */
public record ClearScanPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ClearScanPayload> TYPE = new Type<>(DoolesTools.id("clear_scan"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClearScanPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, ClearScanPayload::pos, ClearScanPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
