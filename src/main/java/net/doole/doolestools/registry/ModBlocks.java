package net.doole.doolestools.registry;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.block.LogisticsComputerBlock;
import net.doole.doolestools.block.LogiGraphWallMonitorBlock;
import net.doole.doolestools.block.LogisticsMonitorBlock;
import net.doole.doolestools.block.NetworkBatteryBlock;
import net.doole.doolestools.block.NetworkModemBlock;
import net.doole.doolestools.block.NetworkGeneratorBlock;
import net.doole.doolestools.block.NetworkRelayBlock;
import net.doole.doolestools.block.NetworkWireBlock;
import net.doole.doolestools.block.WirelessDongleBlock;
import net.doole.doolestools.block.WirelessRouterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DoolesTools.MOD_ID);

    public static final DeferredBlock<LogisticsComputerBlock> LOGISTICS_COMPUTER = BLOCKS.registerBlock(
            "logistics_computer",
            LogisticsComputerBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<LogisticsMonitorBlock> LOGISTICS_MONITOR = BLOCKS.registerBlock(
            "logistics_monitor",
            LogisticsMonitorBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<LogiGraphWallMonitorBlock> LOGIGRAPH_WALL_MONITOR = BLOCKS.registerBlock(
            "logigraph_wall_monitor",
            LogiGraphWallMonitorBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<WirelessRouterBlock> WIRELESS_ROUTER = BLOCKS.registerBlock(
            "wireless_router",
            WirelessRouterBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(0.8F, 2.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<WirelessDongleBlock> WIRELESS_DONGLE = BLOCKS.registerBlock(
            "wireless_dongle",
            WirelessDongleBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(0.8F, 2.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<NetworkModemBlock> NETWORK_MODEM = BLOCKS.registerBlock(
            "network_modem",
            NetworkModemBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(0.8F, 2.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<NetworkWireBlock> NETWORK_WIRE = BLOCKS.registerBlock(
            "network_wire",
            NetworkWireBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(0.4F, 1.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<NetworkRelayBlock> NETWORK_RELAY = BLOCKS.registerBlock(
            "network_relay",
            NetworkRelayBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(1.5F, 4.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<NetworkGeneratorBlock> NETWORK_GENERATOR = BLOCKS.registerBlock(
            "network_generator",
            NetworkGeneratorBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<NetworkBatteryBlock> NETWORK_BATTERY = BLOCKS.registerBlock(
            "network_battery",
            NetworkBatteryBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
    );

}
