package net.doole.doolestools.item;

import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public class NetworkEndpointBlockItem extends BlockItem {
    private final String endpointKind;

    public NetworkEndpointBlockItem(Block block, Properties properties, String endpointKind) {
        super(block, properties);
        this.endpointKind = endpointKind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.NETWORK_WIRE.get())) {
            Direction preferred = context.getClickedFace();
            if (installIntoWire(context, context.getClickedPos(), preferred)) return InteractionResult.SUCCESS;
            if (installIntoWire(context, context.getClickedPos(), context.getClickedFace().getOpposite())) return InteractionResult.SUCCESS;
            return InteractionResult.SUCCESS;
        }

        BlockPos wirePos = context.getClickedPos().relative(context.getClickedFace());
        if (installIntoWire(context, wirePos, context.getClickedFace().getOpposite())) return InteractionResult.SUCCESS;
        return super.useOn(context);
    }

    private boolean installIntoWire(UseOnContext context, BlockPos wirePos, Direction endpointFace) {
        if (!"modem".equals(endpointKind)) return false;
        if (!context.getLevel().getBlockState(wirePos).is(ModBlocks.NETWORK_WIRE.get())) return false;
        if (!(context.getLevel().getBlockEntity(wirePos) instanceof NetworkWireBlockEntity wire)) return false;
        if (wire.hasEndpoint()) return true;
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
}
