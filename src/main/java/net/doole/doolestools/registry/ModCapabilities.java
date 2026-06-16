package net.doole.doolestools.registry;

import net.doole.doolestools.blockentity.NetworkBatteryBlockEntity;
import net.doole.doolestools.blockentity.NetworkGeneratorBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.NETWORK_GENERATOR.get(),
                (NetworkGeneratorBlockEntity generator, net.minecraft.core.Direction side) -> generator.energyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.NETWORK_GENERATOR.get(),
                (NetworkGeneratorBlockEntity generator, net.minecraft.core.Direction side) -> generator.itemHandler());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.NETWORK_BATTERY.get(),
                (NetworkBatteryBlockEntity battery, net.minecraft.core.Direction side) -> battery.energyHandler());
    }
}
