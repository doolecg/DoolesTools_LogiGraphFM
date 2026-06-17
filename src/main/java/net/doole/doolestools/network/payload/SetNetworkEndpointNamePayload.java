package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

public record SetNetworkEndpointNamePayload(BlockPos pos, String name, String networkId, @Nullable Direction face) implements CustomPacketPayload {
    private static final int MAX_NAME_BYTES = 48 * 4;
    // -1 byte sentinel means "no specific face" (applies to all / non-wire targets).
    private static final StreamCodec<RegistryFriendlyByteBuf, Direction> NULLABLE_DIR_CODEC =
            StreamCodec.of(
                    (buf, d) -> buf.writeByte(d == null ? -1 : d.ordinal()),
                    buf -> { byte b = buf.readByte(); return (b & 0xFF) == 0xFF ? null : Direction.values()[b & 7]; });

    public static final Type<SetNetworkEndpointNamePayload> TYPE = new Type<>(DoolesTools.id("set_network_endpoint_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetNetworkEndpointNamePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetNetworkEndpointNamePayload::pos,
                    ByteBufCodecs.stringUtf8(MAX_NAME_BYTES), SetNetworkEndpointNamePayload::name,
                    ByteBufCodecs.stringUtf8(128), SetNetworkEndpointNamePayload::networkId,
                    NULLABLE_DIR_CODEC, SetNetworkEndpointNamePayload::face,
                    SetNetworkEndpointNamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
