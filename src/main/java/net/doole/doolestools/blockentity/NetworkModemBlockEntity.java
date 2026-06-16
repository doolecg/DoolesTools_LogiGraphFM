package net.doole.doolestools.blockentity;

import net.doole.doolestools.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkModemBlockEntity extends NetworkEndpointBlockEntity {
    public NetworkModemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_MODEM.get(), pos, state);
    }

    @Override
    public String deviceKind() {
        return "Network Modem";
    }
}
