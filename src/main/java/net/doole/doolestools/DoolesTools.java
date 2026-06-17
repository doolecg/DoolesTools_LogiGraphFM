package net.doole.doolestools;

import com.mojang.logging.LogUtils;
import net.doole.doolestools.config.ModClientConfig;
import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.integration.IntegrationHooks;
import net.doole.doolestools.network.ModNetworking;
import net.doole.doolestools.registry.ModBlockEntities;
import net.doole.doolestools.registry.ModBlocks;
import net.doole.doolestools.registry.ModCapabilities;
import net.doole.doolestools.registry.ModCreativeTabs;
import net.doole.doolestools.registry.ModItems;
import net.doole.doolestools.registry.ModMenus;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Doole's Tools — an expandable utility / QOL mod.
 *
 * <p>Core feature: <b>LogiGraph</b>, a read-only diagnostics and planning tool. The player places a
 * {@code Logistics Computer}, scans nearby blocks, and manually builds a node flowgraph that can be
 * mirrored on a {@code Logistics Monitor}.</p>
 *
 * <p>LogiGraph stays strictly read-only; saved labels and graph layout are the only persistent edits.</p>
 */
@Mod(DoolesTools.MOD_ID)
public final class DoolesTools {
    public static final String MOD_ID = "doolestools";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DoolesTools(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, ModServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC);

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(DoolesTools::commonSetup);
        modBus.addListener(ModCapabilities::register);
        modBus.addListener(ModNetworking::registerPayloads);

        // Soft integration discovery. Never hard-crashes if optional integrations are absent.
        IntegrationHooks.init(modBus);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> LOGGER.info("Doole's Tools loaded — LogiGraph online."));
    }

    /** Central helper so the (possibly renamed) identifier type is referenced in exactly one place. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
