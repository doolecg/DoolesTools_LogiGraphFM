package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.logistics.switchboard.SwitchboardLinkData;
import net.doole.doolestools.logistics.switchboard.SwitchboardNodePositionData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record SaveSwitchboardPayload(BlockPos pos, List<SwitchboardLinkData> links,
                                     List<SwitchboardNodePositionData> nodePositions) implements CustomPacketPayload {
    public static final Type<SaveSwitchboardPayload> TYPE = new Type<>(DoolesTools.id("save_switchboard"));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<SwitchboardLinkData>> LINKS =
            ByteBufCodecs.fromCodecWithRegistries(SwitchboardLinkData.CODEC.listOf());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<SwitchboardNodePositionData>> NODE_POSITIONS =
            ByteBufCodecs.fromCodecWithRegistries(SwitchboardNodePositionData.CODEC.listOf());
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveSwitchboardPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, SaveSwitchboardPayload::pos,
                    LINKS, SaveSwitchboardPayload::links,
                    NODE_POSITIONS, SaveSwitchboardPayload::nodePositions,
                    SaveSwitchboardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
