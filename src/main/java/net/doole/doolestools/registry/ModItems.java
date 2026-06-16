package net.doole.doolestools.registry;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.item.LabelGunItem;
import net.doole.doolestools.item.NetworkEndpointBlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DoolesTools.MOD_ID);

    public static final DeferredItem<BlockItem> LOGISTICS_COMPUTER =
            ITEMS.registerSimpleBlockItem("logistics_computer", ModBlocks.LOGISTICS_COMPUTER);

    public static final DeferredItem<BlockItem> LOGISTICS_MONITOR =
            ITEMS.registerSimpleBlockItem("logistics_monitor", ModBlocks.LOGISTICS_MONITOR);

    public static final DeferredItem<Item> LABEL_GUN =
            ITEMS.registerItem("label_gun", LabelGunItem::new);

    public static final DeferredItem<Item> NETWORK_SCREWDRIVER =
            ITEMS.registerSimpleItem("network_screwdriver");

    public static final DeferredItem<BlockItem> LOGIGRAPH_WALL_MONITOR =
            ITEMS.registerSimpleBlockItem("logigraph_wall_monitor", ModBlocks.LOGIGRAPH_WALL_MONITOR);

    public static final DeferredItem<NetworkEndpointBlockItem> WIRELESS_ROUTER =
            ITEMS.registerItem("wireless_router", props -> new NetworkEndpointBlockItem(ModBlocks.WIRELESS_ROUTER.get(), props, "router"));

    public static final DeferredItem<NetworkEndpointBlockItem> NETWORK_MODEM =
            ITEMS.registerItem("network_modem", props -> new NetworkEndpointBlockItem(ModBlocks.NETWORK_MODEM.get(), props, "modem"));

    public static final DeferredItem<BlockItem> NETWORK_WIRE =
            ITEMS.registerSimpleBlockItem("network_wire", ModBlocks.NETWORK_WIRE);

    public static final DeferredItem<BlockItem> NETWORK_RELAY =
            ITEMS.registerSimpleBlockItem("network_relay", ModBlocks.NETWORK_RELAY);

    public static final DeferredItem<BlockItem> NETWORK_GENERATOR =
            ITEMS.registerSimpleBlockItem("network_generator", ModBlocks.NETWORK_GENERATOR);

    public static final DeferredItem<BlockItem> NETWORK_BATTERY =
            ITEMS.registerSimpleBlockItem("network_battery", ModBlocks.NETWORK_BATTERY);

    public static final DeferredItem<Item> SPEED_UPGRADE_CARD =
            ITEMS.registerSimpleItem("speed_upgrade_card");

    public static final DeferredItem<Item> STACK_UPGRADE_CARD =
            ITEMS.registerSimpleItem("stack_upgrade_card");

    public static final DeferredItem<Item> RANGE_UPGRADE_CARD =
            ITEMS.registerSimpleItem("range_upgrade_card");

    public static final DeferredItem<Item> EFFICIENCY_UPGRADE_CARD =
            ITEMS.registerSimpleItem("efficiency_upgrade_card");

    public static String upgradeType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        Item item = stack.getItem();
        if (item == SPEED_UPGRADE_CARD.get()) return "speed";
        if (item == STACK_UPGRADE_CARD.get()) return "stack";
        if (item == RANGE_UPGRADE_CARD.get()) return "range";
        if (item == EFFICIENCY_UPGRADE_CARD.get()) return "efficiency";
        return "";
    }

    public static Item upgradeCard(String type) {
        return switch (type == null ? "" : type) {
            case "speed" -> SPEED_UPGRADE_CARD.get();
            case "stack" -> STACK_UPGRADE_CARD.get();
            case "range" -> RANGE_UPGRADE_CARD.get();
            case "efficiency" -> EFFICIENCY_UPGRADE_CARD.get();
            default -> null;
        };
    }

    public static String upgradeLabel(String type) {
        return switch (type == null ? "" : type) {
            case "speed" -> "Speed";
            case "stack" -> "Stack";
            case "range" -> "Range";
            case "efficiency" -> "Efficiency";
            default -> "Upgrade";
        };
    }
}
