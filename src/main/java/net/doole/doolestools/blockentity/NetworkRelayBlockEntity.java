package net.doole.doolestools.blockentity;

import net.doole.doolestools.item.UpgradeType;
import net.doole.doolestools.logistics.network.NetworkNodeIndex;
import net.doole.doolestools.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkRelayBlockEntity extends NetworkDeviceBlockEntity {

    public NetworkRelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_RELAY.get(), pos, state);
    }

    // Join/leave the per-dimension node index so the power calculator can find relays without
    // scanning chunks. onLoad fires on placement and chunk load; setRemoved on break and unload.
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) NetworkNodeIndex.addRelay(serverLevel, this);
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) NetworkNodeIndex.removeRelay(serverLevel, worldPosition);
        super.setRemoved();
    }

    @Override protected String idKey()     { return "relayId"; }
    @Override protected String numberKey() { return "relayNumber"; }
    @Override protected String nameKey()   { return "displayName"; }
    @Override protected boolean supportsUpgrade(UpgradeType type) { return type != UpgradeType.STACK; }

    @Override
    protected String defaultId() {
        return "relay_" + Long.toUnsignedString(worldPosition.asLong());
    }

    @Override
    protected String defaultName() {
        return "Network Relay#" + formattedDeviceId();
    }

    // --- Alias methods so existing callers keep working without changes ---

    public String relayId()          { return deviceId(); }
    public int    relayNumber()      { return deviceNumber(); }
    public String formattedRelayId() { return formattedDeviceId(); }
    public String displayName()      { return deviceName(); }
}
