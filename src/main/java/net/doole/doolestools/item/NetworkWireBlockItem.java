package net.doole.doolestools.item;

import net.doole.doolestools.block.NetworkEndpointBlock;
import net.doole.doolestools.block.NetworkWireBlock;
import net.doole.doolestools.blockentity.NetworkEndpointBlockEntity;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkWireBlockItem extends BlockItem {
    public NetworkWireBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.NETWORK_WIRE.get())
                && level.getBlockEntity(pos) instanceof NetworkWireBlockEntity wire
                && !wire.hasCable()) {
            if (!level.isClientSide()) installCable(context, level, pos, wire);
            return InteractionResult.SUCCESS;
        }
        if (state.is(ModBlocks.NETWORK_MODEM.get())
                && level.getBlockEntity(pos) instanceof NetworkEndpointBlockEntity endpoint) {
            Direction endpointFace = state.getValue(NetworkEndpointBlock.FACING).getOpposite();
            if (!level.isClientSide()) convertSocketToWire(context, level, pos, endpointFace, endpoint);
            return InteractionResult.SUCCESS;
        }
        BlockPos targetPos = pos.relative(context.getClickedFace());
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.is(ModBlocks.NETWORK_WIRE.get())
                && level.getBlockEntity(targetPos) instanceof NetworkWireBlockEntity wire
                && !wire.hasCable()) {
            if (!level.isClientSide()) installCable(context, level, targetPos, wire);
            return InteractionResult.SUCCESS;
        }
        if (targetState.is(ModBlocks.NETWORK_MODEM.get())
                && level.getBlockEntity(targetPos) instanceof NetworkEndpointBlockEntity endpoint) {
            Direction endpointFace = targetState.getValue(NetworkEndpointBlock.FACING).getOpposite();
            if (!level.isClientSide()) convertSocketToWire(context, level, targetPos, endpointFace, endpoint);
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    private static void installCable(UseOnContext context, Level level, BlockPos pos, NetworkWireBlockEntity wire) {
        wire.setCableInstalled(true);
        level.setBlock(pos, NetworkWireBlock.withConnections(level, pos, wire.getBlockState().setValue(NetworkWireBlock.CABLE, true)), 3);
        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) stack.shrink(1);
    }

    private static void convertSocketToWire(UseOnContext context, Level level, BlockPos pos,
            Direction endpointFace, NetworkEndpointBlockEntity endpoint) {
        String name = endpoint.deviceName();
        String networkId = endpoint.networkId();
        int[] upgrades = endpoint.upgradeCounts();

        BlockState wireState = NetworkWireBlock.withConnections(level, pos, ModBlocks.NETWORK_WIRE.get().defaultBlockState());
        level.setBlock(pos, wireState, 3);
        if (!(level.getBlockEntity(pos) instanceof NetworkWireBlockEntity wire)) return;
        wire.setCableInstalled(true);
        if (!wire.installEndpoint("modem", endpointFace, name)) return;
        wire.setEndpointIdentityAt(endpointFace, name, networkId);
        for (UpgradeType type : UpgradeType.values()) {
            int count = switch (type) {
                case SPEED -> upgrades.length > 0 ? upgrades[0] : 0;
                case STACK -> upgrades.length > 1 ? upgrades[1] : 0;
                case RANGE -> upgrades.length > 2 ? upgrades[2] : 0;
                case EFFICIENCY -> upgrades.length > 3 ? upgrades[3] : 0;
            };
            for (int i = 0; i < count; i++) wire.installUpgrade(type, endpointFace);
        }
        level.setBlock(pos, NetworkWireBlock.withConnections(level, pos, wire.getBlockState()), 3);

        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) stack.shrink(1);
    }
}
