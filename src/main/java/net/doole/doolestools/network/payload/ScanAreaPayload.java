package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: player pressed "Scan Area". Triggers a manual, read-only scan server-side. */
public record ScanAreaPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ScanAreaPayload> TYPE = new Type<>(DoolesTools.id("scan_area"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScanAreaPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, ScanAreaPayload::pos, ScanAreaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
