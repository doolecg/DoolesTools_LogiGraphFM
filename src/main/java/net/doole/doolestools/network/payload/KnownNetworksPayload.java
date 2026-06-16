package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record KnownNetworksPayload(List<String> ids, List<String> names, List<Boolean> editable) implements CustomPacketPayload {
    public static final Type<KnownNetworksPayload> TYPE = new Type<>(DoolesTools.id("known_networks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KnownNetworksPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), KnownNetworksPayload::ids,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), KnownNetworksPayload::names,
                    ByteBufCodecs.BOOL.apply(ByteBufCodecs.list()), KnownNetworksPayload::editable,
                    KnownNetworksPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
