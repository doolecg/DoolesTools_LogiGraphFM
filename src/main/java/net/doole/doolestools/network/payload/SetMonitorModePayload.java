package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: change the monitor's display mode (0=Flowgraph, 1=Warnings, 2=Storage Summary). */
public record SetMonitorModePayload(BlockPos pos, int mode) implements CustomPacketPayload {
    public static final Type<SetMonitorModePayload> TYPE = new Type<>(DoolesTools.id("set_monitor_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetMonitorModePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetMonitorModePayload::pos,
                    ByteBufCodecs.VAR_INT, SetMonitorModePayload::mode,
                    SetMonitorModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
