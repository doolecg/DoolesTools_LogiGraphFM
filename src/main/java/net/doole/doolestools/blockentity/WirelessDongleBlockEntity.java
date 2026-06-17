package net.doole.doolestools.blockentity;

import net.doole.doolestools.logistics.network.NetworkNodeIndex;
import net.doole.doolestools.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class WirelessDongleBlockEntity extends NetworkEndpointBlockEntity {
    public WirelessDongleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_DONGLE.get(), pos, state);
    }

    @Override
    public String deviceKind() {
        return "Wireless Dongle";
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) NetworkNodeIndex.addRouter(serverLevel, this);
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) NetworkNodeIndex.removeRouter(serverLevel, worldPosition);
        super.setRemoved();
    }
}
