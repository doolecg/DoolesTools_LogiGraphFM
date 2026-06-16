package net.doole.doolestools.blockentity;

import net.doole.doolestools.logistics.network.NetworkNodeIndex;
import net.doole.doolestools.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class WirelessRouterBlockEntity extends NetworkEndpointBlockEntity {
    public WirelessRouterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_ROUTER.get(), pos, state);
    }

    @Override
    public String deviceKind() {
        return "Wireless Router";
    }

    // Standalone routers register with the node index; wire-mounted endpoints are found via the
    // wired flood-fill instead, so only this block entity type needs to be tracked here.
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
