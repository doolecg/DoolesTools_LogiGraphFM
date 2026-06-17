package net.doole.doolestools.client;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.client.screen.LogisticsComputerScreen;
import net.doole.doolestools.client.screen.LogisticsMonitorScreen;
import net.doole.doolestools.client.screen.NetworkGeneratorScreen;
import net.doole.doolestools.client.screen.NetworkSwitchboardScreen;
import net.doole.doolestools.client.render.LogiGraphWallMonitorRenderer;
import net.doole.doolestools.config.ModClientConfig;
import net.doole.doolestools.registry.ModBlockEntities;
import net.doole.doolestools.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only setup: binds menus to their custom screens. */
@EventBusSubscriber(modid = DoolesTools.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.LOGISTICS_COMPUTER.get(), LogisticsComputerScreen::new);
        event.register(ModMenus.LOGISTICS_MONITOR.get(), LogisticsMonitorScreen::new);
        event.register(ModMenus.NETWORK_GENERATOR.get(), NetworkGeneratorScreen::new);
        event.register(ModMenus.NETWORK_BATTERY.get(), net.doole.doolestools.client.screen.NetworkBatteryScreen::new);
        event.register(ModMenus.NETWORK_SWITCHBOARD.get(), NetworkSwitchboardScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LOGIGRAPH_WALL_MONITOR.get(), LogiGraphWallMonitorRenderer::new);
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ModClientConfig.SPEC) ClientPrefs.load();
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ModClientConfig.SPEC) ClientPrefs.load();
    }
}
