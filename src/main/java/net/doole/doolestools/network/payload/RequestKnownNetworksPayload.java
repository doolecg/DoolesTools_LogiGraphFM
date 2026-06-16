package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestKnownNetworksPayload() implements CustomPacketPayload {
    public static final Type<RequestKnownNetworksPayload> TYPE = new Type<>(DoolesTools.id("request_known_networks"));
    public static final RequestKnownNetworksPayload INSTANCE = new RequestKnownNetworksPayload();

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestKnownNetworksPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
