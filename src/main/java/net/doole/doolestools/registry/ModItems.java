package net.doole.doolestools.registry;

import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.item.LabelGunItem;
import net.doole.doolestools.item.LinkingCardItem;
import net.doole.doolestools.item.NetworkEndpointBlockItem;
import net.doole.doolestools.item.NetworkScrewdriverItem;
import net.doole.doolestools.item.NetworkWireBlockItem;
import net.doole.doolestools.item.UpgradeType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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
            ITEMS.registerItem("network_screwdriver", NetworkScrewdriverItem::new);

    public static final DeferredItem<Item> LINKING_CARD =
            ITEMS.registerItem("linking_card", LinkingCardItem::new);

    // DISABLED: LogiGraph Wall Monitor item commented out per request.
    // public static final DeferredItem<BlockItem> LOGIGRAPH_WALL_MONITOR =
    //         ITEMS.registerSimpleBlockItem("logigraph_wall_monitor", ModBlocks.LOGIGRAPH_WALL_MONITOR);

    public static final DeferredItem<NetworkEndpointBlockItem> WIRELESS_ROUTER =
            ITEMS.registerItem("wireless_router", props -> new NetworkEndpointBlockItem(ModBlocks.WIRELESS_ROUTER.get(), props, "router"));

    public static final DeferredItem<NetworkEndpointBlockItem> NETWORK_MODEM =
            ITEMS.registerItem("network_modem", props -> new NetworkEndpointBlockItem(ModBlocks.NETWORK_MODEM.get(), props, "modem"));

    public static final DeferredItem<NetworkEndpointBlockItem> WIRELESS_DONGLE =
            ITEMS.registerItem("wireless_dongle", props -> new NetworkEndpointBlockItem(ModBlocks.WIRELESS_DONGLE.get(), props, "dongle"));

    public static final DeferredItem<NetworkWireBlockItem> NETWORK_WIRE =
            ITEMS.registerItem("network_wire", props -> new NetworkWireBlockItem(ModBlocks.NETWORK_WIRE.get(), props));

    public static final DeferredItem<BlockItem> NETWORK_RELAY =
            ITEMS.registerSimpleBlockItem("network_relay", ModBlocks.NETWORK_RELAY);

    public static final DeferredItem<BlockItem> NETWORK_GENERATOR =
            ITEMS.registerSimpleBlockItem("network_generator", ModBlocks.NETWORK_GENERATOR);

    public static final DeferredItem<BlockItem> NETWORK_BATTERY =
            ITEMS.registerSimpleBlockItem("network_battery", ModBlocks.NETWORK_BATTERY);

    public static final DeferredItem<BlockItem> NETWORK_SWITCHBOARD =
            ITEMS.registerSimpleBlockItem("network_switchboard", ModBlocks.NETWORK_SWITCHBOARD);

    public static final DeferredItem<Item> SPEED_UPGRADE_CARD      = ITEMS.registerSimpleItem("speed_upgrade_card");
    public static final DeferredItem<Item> STACK_UPGRADE_CARD      = ITEMS.registerSimpleItem("stack_upgrade_card");
    public static final DeferredItem<Item> RANGE_UPGRADE_CARD      = ITEMS.registerSimpleItem("range_upgrade_card");
    public static final DeferredItem<Item> EFFICIENCY_UPGRADE_CARD = ITEMS.registerSimpleItem("efficiency_upgrade_card");

    /** Returns the {@link UpgradeType} for the held item, or {@code null} if not an upgrade card. */
    @Nullable
    public static UpgradeType upgradeType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item == SPEED_UPGRADE_CARD.get())      return UpgradeType.SPEED;
        if (item == STACK_UPGRADE_CARD.get())      return UpgradeType.STACK;
        if (item == RANGE_UPGRADE_CARD.get())      return UpgradeType.RANGE;
        if (item == EFFICIENCY_UPGRADE_CARD.get()) return UpgradeType.EFFICIENCY;
        return null;
    }

    /** Returns the upgrade card item for the given type, or {@code null} for an unknown type. */
    @Nullable
    public static Item upgradeCard(UpgradeType type) {
        if (type == null) return null;
        return switch (type) {
            case SPEED      -> SPEED_UPGRADE_CARD.get();
            case STACK      -> STACK_UPGRADE_CARD.get();
            case RANGE      -> RANGE_UPGRADE_CARD.get();
            case EFFICIENCY -> EFFICIENCY_UPGRADE_CARD.get();
        };
    }

    /** Returns the upgrade card item for the given id string. Kept for legacy callers. */
    @Nullable
    public static Item upgradeCard(String id) {
        return upgradeCard(UpgradeType.byId(id));
    }
}
