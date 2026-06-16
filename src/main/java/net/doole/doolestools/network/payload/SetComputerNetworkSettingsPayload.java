package net.doole.doolestools.network.payload;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.network.ModStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record SetComputerNetworkSettingsPayload(BlockPos pos, String name, String accessMode, List<String> editorWhitelist) implements CustomPacketPayload {
    public static final Type<SetComputerNetworkSettingsPayload> TYPE = new Type<>(DoolesTools.id("set_computer_network_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetComputerNetworkSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetComputerNetworkSettingsPayload::pos,
                    ByteBufCodecs.stringUtf8(192), SetComputerNetworkSettingsPayload::name,
                    ByteBufCodecs.stringUtf8(32), SetComputerNetworkSettingsPayload::accessMode,
                    ModStreamCodecs.STRING_LIST, SetComputerNetworkSettingsPayload::editorWhitelist,
                    SetComputerNetworkSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
