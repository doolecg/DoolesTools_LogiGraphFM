package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: ask the server for block labels near the player (for the Label Gun hologram overlay). */
public record RequestNearbyLabelsPayload() implements CustomPacketPayload {
    public static final Type<RequestNearbyLabelsPayload> TYPE = new Type<>(DoolesTools.id("request_nearby_labels"));

    public static final RequestNearbyLabelsPayload INSTANCE = new RequestNearbyLabelsPayload();

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestNearbyLabelsPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
