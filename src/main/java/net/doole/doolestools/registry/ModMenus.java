package net.doole.doolestools.registry;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.menu.LogisticsComputerMenu;
import net.doole.doolestools.menu.LogisticsMonitorMenu;
import net.doole.doolestools.menu.NetworkBatteryMenu;
import net.doole.doolestools.menu.NetworkGeneratorMenu;
import net.doole.doolestools.menu.NetworkSwitchboardMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, DoolesTools.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<LogisticsComputerMenu>> LOGISTICS_COMPUTER =
            MENUS.register("logistics_computer", () -> IMenuTypeExtension.create(LogisticsComputerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LogisticsMonitorMenu>> LOGISTICS_MONITOR =
            MENUS.register("logistics_monitor", () -> IMenuTypeExtension.create(LogisticsMonitorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NetworkGeneratorMenu>> NETWORK_GENERATOR =
            MENUS.register("network_generator", () -> IMenuTypeExtension.create(NetworkGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NetworkBatteryMenu>> NETWORK_BATTERY =
            MENUS.register("network_battery", () -> IMenuTypeExtension.create(NetworkBatteryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NetworkSwitchboardMenu>> NETWORK_SWITCHBOARD =
            MENUS.register("network_switchboard", () -> IMenuTypeExtension.create(NetworkSwitchboardMenu::new));

}
