package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.logistics.switchboard.SwitchboardLinkData;
import net.doole.doolestools.logistics.switchboard.SwitchboardNodePositionData;
import net.doole.doolestools.network.ModStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record SwitchboardStatePayload(BlockPos pos, List<SwitchboardLinkData> links,
                                      List<SwitchboardNodePositionData> nodePositions,
                                      List<Integer> packetHistory,
                                      List<Integer> powerHistory,
                                      List<Integer> itemHistory,
                                      int activeRoutes) implements CustomPacketPayload {
    public static final Type<SwitchboardStatePayload> TYPE = new Type<>(DoolesTools.id("switchboard_state"));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<SwitchboardLinkData>> LINKS =
            ByteBufCodecs.fromCodecWithRegistries(SwitchboardLinkData.CODEC.listOf());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<SwitchboardNodePositionData>> NODE_POSITIONS =
            ByteBufCodecs.fromCodecWithRegistries(SwitchboardNodePositionData.CODEC.listOf());
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchboardStatePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SwitchboardStatePayload decode(RegistryFriendlyByteBuf buf) {
                    return new SwitchboardStatePayload(
                            BlockPos.STREAM_CODEC.decode(buf),
                            LINKS.decode(buf),
                            NODE_POSITIONS.decode(buf),
                            ModStreamCodecs.INT_LIST.decode(buf),
                            ModStreamCodecs.INT_LIST.decode(buf),
                            ModStreamCodecs.INT_LIST.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SwitchboardStatePayload payload) {
                    BlockPos.STREAM_CODEC.encode(buf, payload.pos());
                    LINKS.encode(buf, payload.links());
                    NODE_POSITIONS.encode(buf, payload.nodePositions());
                    ModStreamCodecs.INT_LIST.encode(buf, payload.packetHistory());
                    ModStreamCodecs.INT_LIST.encode(buf, payload.powerHistory());
                    ModStreamCodecs.INT_LIST.encode(buf, payload.itemHistory());
                    ByteBufCodecs.VAR_INT.encode(buf, payload.activeRoutes());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
