package net.doole.doolestools.item;

import net.doole.doolestools.block.NetworkWireBlock;
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

public class NetworkEndpointBlockItem extends BlockItem {
    private final String endpointKind;

    public NetworkEndpointBlockItem(Block block, Properties properties, String endpointKind) {
        super(block, properties);
        this.endpointKind = endpointKind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.NETWORK_WIRE.get())) {
            if (installIntoWire(context, context.getClickedPos(), context.getClickedFace())) return InteractionResult.SUCCESS;
            return InteractionResult.SUCCESS;
        }

        BlockPos wirePos = context.getClickedPos().relative(context.getClickedFace());
        Direction endpointFace = context.getClickedFace().getOpposite();
        if (installIntoWire(context, wirePos, endpointFace)) return InteractionResult.SUCCESS;
        if (createEndpointHost(context, wirePos, endpointFace)) return InteractionResult.SUCCESS;
        return super.useOn(context);
    }

    private boolean installIntoWire(UseOnContext context, BlockPos wirePos, Direction endpointFace) {
        if (!"modem".equals(endpointKind)) return false;
        if (!context.getLevel().getBlockState(wirePos).is(ModBlocks.NETWORK_WIRE.get())) return false;
        if (!(context.getLevel().getBlockEntity(wirePos) instanceof NetworkWireBlockEntity wire)) return false;
        if (!isEndpointTarget(context.getLevel(), wirePos.relative(endpointFace))) return false;
        if (wire.hasEndpointAt(endpointFace)) return false;
        if (!context.getLevel().isClientSide()) {
            ItemStack stack = context.getItemInHand();
            if (wire.installEndpoint(endpointKind, endpointFace, "")
                    && context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                stack.shrink(1);
            }
            context.getLevel().sendBlockUpdated(wirePos, wire.getBlockState(), wire.getBlockState(), 3);
        }
        return true;
    }

    private static boolean isEndpointTarget(Level level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) return false;
        return !(level.getBlockEntity(pos) instanceof NetworkWireBlockEntity)
                && !(level.getBlockEntity(pos) instanceof net.doole.doolestools.blockentity.NetworkEndpointBlockEntity);
    }

    private boolean createEndpointHost(UseOnContext context, BlockPos wirePos, Direction endpointFace) {
        if (!"modem".equals(endpointKind)) return false;
        Level level = context.getLevel();
        if (!level.getBlockState(wirePos).isAir()) return false;
        if (!level.getBlockState(wirePos.relative(endpointFace)).isAir()) {
            if (!level.isClientSide()) {
                BlockState hostState = ModBlocks.NETWORK_WIRE.get().defaultBlockState().setValue(NetworkWireBlock.CABLE, false);
                level.setBlock(wirePos, hostState, 3);
                if (level.getBlockEntity(wirePos) instanceof NetworkWireBlockEntity wire) {
                    wire.setCableInstalled(false);
                    if (!wire.installEndpoint(endpointKind, endpointFace, "")) return true;
                    ItemStack stack = context.getItemInHand();
                    if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) stack.shrink(1);
                }
            }
            return true;
        }
        return false;
    }
}
