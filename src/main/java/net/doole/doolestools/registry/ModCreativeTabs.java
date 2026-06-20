package net.doole.doolestools.registry;

import net.doole.doolestools.DoolesTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DoolesTools.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.doolestools.main"))
                    .icon(() -> new ItemStack(ModItems.LOGISTICS_COMPUTER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.LOGISTICS_COMPUTER.get());
                        output.accept(ModItems.LOGISTICS_MONITOR.get());
                        output.accept(ModItems.LABEL_GUN.get());
                        output.accept(ModItems.NETWORK_SCREWDRIVER.get());
                        output.accept(ModItems.LINKING_CARD.get());
                        // DISABLED: output.accept(ModItems.LOGIGRAPH_WALL_MONITOR.get());
                        output.accept(ModItems.WIRELESS_ROUTER.get());
                        output.accept(ModItems.WIRELESS_DONGLE.get());
                        output.accept(ModItems.NETWORK_MODEM.get());
                        output.accept(ModItems.NETWORK_WIRE.get());
                        output.accept(ModItems.NETWORK_RELAY.get());
                        output.accept(ModItems.NETWORK_GENERATOR.get());
                        output.accept(ModItems.NETWORK_BATTERY.get());
                        output.accept(ModItems.NETWORK_SWITCHBOARD.get());
                        output.accept(ModItems.SPEED_UPGRADE_CARD.get());
                        output.accept(ModItems.STACK_UPGRADE_CARD.get());
                        output.accept(ModItems.RANGE_UPGRADE_CARD.get());
                        output.accept(ModItems.EFFICIENCY_UPGRADE_CARD.get());
                    })
                    .build());
}
