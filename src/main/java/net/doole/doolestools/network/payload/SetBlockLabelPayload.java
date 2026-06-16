package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: set or clear a server-owned block label. */
public record SetBlockLabelPayload(BlockPos pos, String label) implements CustomPacketPayload {
    /** Maximum UTF-8 byte length accepted from the client — matches {@link net.doole.doolestools.world.BlockLabelSavedData#MAX_LABEL}. */
    private static final int MAX_LABEL_BYTES = 48 * 4; // worst-case 4 bytes per char

    public static final Type<SetBlockLabelPayload> TYPE = new Type<>(DoolesTools.id("set_block_label"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetBlockLabelPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetBlockLabelPayload::pos,
                    ByteBufCodecs.stringUtf8(MAX_LABEL_BYTES), SetBlockLabelPayload::label,
                    SetBlockLabelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
