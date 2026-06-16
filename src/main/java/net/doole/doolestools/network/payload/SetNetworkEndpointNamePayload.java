package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetNetworkEndpointNamePayload(BlockPos pos, String name, String networkId) implements CustomPacketPayload {
    private static final int MAX_NAME_BYTES = 48 * 4;

    public static final Type<SetNetworkEndpointNamePayload> TYPE = new Type<>(DoolesTools.id("set_network_endpoint_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetNetworkEndpointNamePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetNetworkEndpointNamePayload::pos,
                    ByteBufCodecs.stringUtf8(MAX_NAME_BYTES), SetNetworkEndpointNamePayload::name,
                    ByteBufCodecs.stringUtf8(128), SetNetworkEndpointNamePayload::networkId,
                    SetNetworkEndpointNamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
