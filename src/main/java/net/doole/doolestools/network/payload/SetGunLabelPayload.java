package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: store a name on the held Label Gun. The gun keeps this name until it is changed. */
public record SetGunLabelPayload(String label) implements CustomPacketPayload {
    /** Maximum UTF-8 byte length accepted from the client — same bound used for block labels. */
    private static final int MAX_LABEL_BYTES = 48 * 4; // worst-case 4 bytes per char

    public static final Type<SetGunLabelPayload> TYPE = new Type<>(DoolesTools.id("set_gun_label"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetGunLabelPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(MAX_LABEL_BYTES), SetGunLabelPayload::label, SetGunLabelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
