package net.doole.doolestools.blockentity;

import net.doole.doolestools.block.NetworkEndpointBlock;
import net.doole.doolestools.item.UpgradeType;
import net.doole.doolestools.util.NetworkIdentityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class NetworkEndpointBlockEntity extends NetworkDeviceBlockEntity {

    protected NetworkEndpointBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract String deviceKind();

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && networkId().isBlank())
            setNetworkId(NetworkIdentityUtils.inferNearbyNetwork(serverLevel, worldPosition));
    }

    @Override protected String idKey()     { return "deviceId"; }
    @Override protected String numberKey() { return "deviceNumber"; }
    @Override protected String nameKey()   { return "deviceName"; }
    @Override protected boolean supportsUpgrade(UpgradeType type) { return true; }

    @Override
    protected String defaultId() {
        return deviceKind().toLowerCase(java.util.Locale.ROOT).replace(' ', '_') + "_" + Long.toUnsignedString(worldPosition.asLong());
    }

    @Override
    protected String defaultName() {
        return attachedBaseName() + "#" + formattedDeviceId();
    }

    public void setIdentityFromName(String name) {
        setDeviceName(name);
    }

    public BlockPos attachedPos() {
        return NetworkEndpointBlock.attachedPos(worldPosition, getBlockState());
    }

    private String attachedBaseName() {
        if (level == null) return deviceKind();
        BlockState attachedState = level.getBlockState(attachedPos());
        if (attachedState.isAir()) return deviceKind();
        String blockName = attachedState.getBlock().getName().getString();
        return blockName == null || blockName.isBlank() ? deviceKind() : blockName;
    }

    // --- Static delegates kept so existing callers compile without import changes ---

    public static String sanitize(String value)                             { return NetworkIdentityUtils.sanitize(value); }
    public static String slug(String value)                                 { return NetworkIdentityUtils.slug(value); }
    public static String sanitizeNetworkId(String value)                    { return NetworkIdentityUtils.sanitizeNetworkId(value); }
    public static String inferNearbyNetwork(ServerLevel level, BlockPos pos) { return NetworkIdentityUtils.inferNearbyNetwork(level, pos); }
}
