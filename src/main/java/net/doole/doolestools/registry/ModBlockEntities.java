package net.doole.doolestools.registry;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.blockentity.LogisticsComputerBlockEntity;
import net.doole.doolestools.blockentity.LogiGraphWallMonitorBlockEntity;
import net.doole.doolestools.blockentity.LogisticsMonitorBlockEntity;
import net.doole.doolestools.blockentity.NetworkBatteryBlockEntity;
import net.doole.doolestools.blockentity.NetworkModemBlockEntity;
import net.doole.doolestools.blockentity.NetworkGeneratorBlockEntity;
import net.doole.doolestools.blockentity.NetworkRelayBlockEntity;
import net.doole.doolestools.blockentity.NetworkSwitchboardBlockEntity;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.blockentity.WirelessDongleBlockEntity;
import net.doole.doolestools.blockentity.WirelessRouterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.BiFunction;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, DoolesTools.MOD_ID);

    // --- Registration helper ---

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>>
    register(String name, DeferredBlock<?> block, BiFunction<BlockPos, BlockState, T> factory) {
        return BLOCK_ENTITY_TYPES.register(name, () -> new BlockEntityType<>(factory::apply, Set.of(block.get())));
    }

    // --- Block entity registrations ---

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsComputerBlockEntity>> LOGISTICS_COMPUTER =
            register("logistics_computer", ModBlocks.LOGISTICS_COMPUTER, LogisticsComputerBlockEntity::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsMonitorBlockEntity>> LOGISTICS_MONITOR =
            register("logistics_monitor", ModBlocks.LOGISTICS_MONITOR, LogisticsMonitorBlockEntity::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogiGraphWallMonitorBlockEntity>> LOGIGRAPH_WALL_MONITOR =
            register("logigraph_wall_monitor", ModBlocks.LOGIGRAPH_WALL_MONITOR, LogiGraphWallMonitorBlockEntity::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirelessRouterBlockEntity>> WIRELESS_ROUTER =
            register("wireless_router", ModBlocks.WIRELESS_ROUTER, WirelessRouterBlockEntity::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirelessDongleBlockEntity>> WIRELESS_DONGLE =
            register("wireless_dongle", ModBlocks.WIRELESS_DONGLE, WirelessDongleBlockEntity::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkModemBlockEntity>> NETWORK_MODEM =
            register("network_modem", ModBlocks.NETWORK_MODEM, NetworkModemBlockEntity::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkWireBlockEntity>> NETWORK_WIRE =
            register("network_wire", ModBlocks.NETWORK_WIRE, NetworkWireBlockEntity::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkRelayBlockEntity>> NETWORK_RELAY =
            register("network_relay", ModBlocks.NETWORK_RELAY, NetworkRelayBlockEntity::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkGeneratorBlockEntity>> NETWORK_GENERATOR =
            register("network_generator", ModBlocks.NETWORK_GENERATOR, NetworkGeneratorBlockEntity::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkBatteryBlockEntity>> NETWORK_BATTERY =
            register("network_battery", ModBlocks.NETWORK_BATTERY, NetworkBatteryBlockEntity::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkSwitchboardBlockEntity>> NETWORK_SWITCHBOARD =
            register("network_switchboard", ModBlocks.NETWORK_SWITCHBOARD, NetworkSwitchboardBlockEntity::new);
}
