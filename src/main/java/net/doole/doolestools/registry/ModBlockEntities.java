package net.doole.doolestools.registry;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.blockentity.LogisticsComputerBlockEntity;
import net.doole.doolestools.blockentity.LogiGraphWallMonitorBlockEntity;
import net.doole.doolestools.blockentity.LogisticsMonitorBlockEntity;
import net.doole.doolestools.blockentity.NetworkBatteryBlockEntity;
import net.doole.doolestools.blockentity.NetworkModemBlockEntity;
import net.doole.doolestools.blockentity.NetworkGeneratorBlockEntity;
import net.doole.doolestools.blockentity.NetworkRelayBlockEntity;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.blockentity.WirelessDongleBlockEntity;
import net.doole.doolestools.blockentity.WirelessRouterBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, DoolesTools.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsComputerBlockEntity>> LOGISTICS_COMPUTER =
            BLOCK_ENTITY_TYPES.register("logistics_computer", () ->
                    new BlockEntityType<>(LogisticsComputerBlockEntity::new, Set.of(ModBlocks.LOGISTICS_COMPUTER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsMonitorBlockEntity>> LOGISTICS_MONITOR =
            BLOCK_ENTITY_TYPES.register("logistics_monitor", () ->
                    new BlockEntityType<>(LogisticsMonitorBlockEntity::new, Set.of(ModBlocks.LOGISTICS_MONITOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogiGraphWallMonitorBlockEntity>> LOGIGRAPH_WALL_MONITOR =
            BLOCK_ENTITY_TYPES.register("logigraph_wall_monitor", () ->
                    new BlockEntityType<>(LogiGraphWallMonitorBlockEntity::new, Set.of(ModBlocks.LOGIGRAPH_WALL_MONITOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirelessRouterBlockEntity>> WIRELESS_ROUTER =
            BLOCK_ENTITY_TYPES.register("wireless_router", () ->
                    new BlockEntityType<>(WirelessRouterBlockEntity::new, Set.of(ModBlocks.WIRELESS_ROUTER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirelessDongleBlockEntity>> WIRELESS_DONGLE =
            BLOCK_ENTITY_TYPES.register("wireless_dongle", () ->
                    new BlockEntityType<>(WirelessDongleBlockEntity::new, Set.of(ModBlocks.WIRELESS_DONGLE.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkModemBlockEntity>> NETWORK_MODEM =
            BLOCK_ENTITY_TYPES.register("network_modem", () ->
                    new BlockEntityType<>(NetworkModemBlockEntity::new, Set.of(ModBlocks.NETWORK_MODEM.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkWireBlockEntity>> NETWORK_WIRE =
            BLOCK_ENTITY_TYPES.register("network_wire", () ->
                    new BlockEntityType<>(NetworkWireBlockEntity::new, Set.of(ModBlocks.NETWORK_WIRE.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkRelayBlockEntity>> NETWORK_RELAY =
            BLOCK_ENTITY_TYPES.register("network_relay", () ->
                    new BlockEntityType<>(NetworkRelayBlockEntity::new, Set.of(ModBlocks.NETWORK_RELAY.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkGeneratorBlockEntity>> NETWORK_GENERATOR =
            BLOCK_ENTITY_TYPES.register("network_generator", () ->
                    new BlockEntityType<>(NetworkGeneratorBlockEntity::new, Set.of(ModBlocks.NETWORK_GENERATOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkBatteryBlockEntity>> NETWORK_BATTERY =
            BLOCK_ENTITY_TYPES.register("network_battery", () ->
                    new BlockEntityType<>(NetworkBatteryBlockEntity::new, Set.of(ModBlocks.NETWORK_BATTERY.get())));
}
