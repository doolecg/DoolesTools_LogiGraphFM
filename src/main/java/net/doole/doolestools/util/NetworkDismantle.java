package net.doole.doolestools.util;

import net.doole.doolestools.block.NetworkWireBlock;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.item.UpgradeType;
import net.doole.doolestools.registry.ModBlocks;
import net.doole.doolestools.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class NetworkDismantle {
    private static final TagKey<Item> COMMON_WRENCH = itemTag("c", "tools/wrench");
    private static final TagKey<Item> COMMON_WRENCHES = itemTag("c", "wrenches");
    private static final TagKey<Item> FORGE_WRENCH = itemTag("forge", "tools/wrench");
    private static final TagKey<Item> FORGE_WRENCHES = itemTag("forge", "wrenches");

    private NetworkDismantle() {
    }

    public static boolean tryDismantle(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (player == null || !player.isShiftKeyDown() || !isWrench(stack)) return false;
        BlockState state = level.getBlockState(pos);
        if (!isDismantleable(state)) return false;
        if (!level.isClientSide()) {
            if (state.getBlock() instanceof NetworkWireBlock && level.getBlockEntity(pos) instanceof NetworkWireBlockEntity wire) {
                dismantleWire(level, pos, player, wire);
            } else {
                level.destroyBlock(pos, !player.getAbilities().instabuild, player);
            }
        }
        return true;
    }

    public static boolean isWrench(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() == ModItems.NETWORK_SCREWDRIVER.get()
                || stack.is(COMMON_WRENCH)
                || stack.is(COMMON_WRENCHES)
                || stack.is(FORGE_WRENCH)
                || stack.is(FORGE_WRENCHES));
    }

    private static boolean isDismantleable(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.LOGISTICS_COMPUTER.get()
                || block == ModBlocks.LOGISTICS_MONITOR.get()
                // DISABLED: || block == ModBlocks.LOGIGRAPH_WALL_MONITOR.get()
                || block == ModBlocks.WIRELESS_ROUTER.get()
                || block == ModBlocks.WIRELESS_DONGLE.get()
                || block == ModBlocks.NETWORK_MODEM.get()
                || block == ModBlocks.NETWORK_WIRE.get()
                || block == ModBlocks.NETWORK_RELAY.get()
                || block == ModBlocks.NETWORK_GENERATOR.get()
                || block == ModBlocks.NETWORK_BATTERY.get()
                || block == ModBlocks.NETWORK_SWITCHBOARD.get();
    }

    private static void dismantleWire(Level level, BlockPos pos, Player player, NetworkWireBlockEntity wire) {
        boolean drop = !player.getAbilities().instabuild;
        if (drop && wire.hasCable()) Block.popResource(level, pos, new ItemStack(ModItems.NETWORK_WIRE.get()));
        if (drop) {
            for (Direction face : wire.endpointFaces()) {
                Block.popResource(level, pos, new ItemStack(ModItems.NETWORK_MODEM.get()));
                for (UpgradeType type : UpgradeType.values()) {
                    Item item = ModItems.upgradeCard(type);
                    int count = wire.upgradeCount(face, type);
                    if (item != null && count > 0) Block.popResource(level, pos, new ItemStack(item, count));
                }
            }
        }
        level.removeBlock(pos, false);
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path));
    }
}
