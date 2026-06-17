package net.doole.doolestools.item;

import net.doole.doolestools.blockentity.LogisticsComputerBlockEntity;
import net.doole.doolestools.blockentity.NetworkEndpointBlockEntity;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LinkingCardItem extends Item {
    private static final String NETWORK_ID = "NetworkId";
    private static final String NETWORK_NAME = "NetworkName";

    public LinkingCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = context.getLevel().getBlockEntity(pos);
        ItemStack stack = context.getItemInHand();
        if (blockEntity instanceof LogisticsComputerBlockEntity computer) {
            storeNetwork(stack, computer.networkId(), computer.networkName());
            player.sendSystemMessage(Component.literal("Linking Card copied " + computer.networkName()));
            return InteractionResult.SUCCESS;
        }

        String networkId = storedNetworkId(stack);
        String networkName = storedNetworkName(stack);
        if (networkId.isBlank()) {
            player.sendSystemMessage(Component.literal("Shift-use a LogiGraph Computer first."));
            return InteractionResult.SUCCESS;
        }
        if (blockEntity instanceof NetworkEndpointBlockEntity endpoint) {
            endpoint.setIdentity(endpoint.deviceName(), networkId);
            context.getLevel().sendBlockUpdated(pos, endpoint.getBlockState(), endpoint.getBlockState(), 3);
            player.sendSystemMessage(Component.literal(endpoint.deviceName() + " linked to " + label(networkId, networkName)));
            return InteractionResult.SUCCESS;
        }
        if (blockEntity instanceof NetworkWireBlockEntity wire && wire.hasEndpoint()) {
            net.minecraft.core.Direction clickedFace = context.getClickedFace();
            if (wire.hasEndpointAt(clickedFace)) {
                wire.setEndpointIdentityAt(clickedFace, wire.endpointName(clickedFace), networkId);
                player.sendSystemMessage(Component.literal(wire.endpointName(clickedFace) + " linked to " + label(networkId, networkName)));
            } else {
                wire.setEndpointIdentity(wire.endpointName(), networkId);
                player.sendSystemMessage(Component.literal(wire.endpointName() + " linked to " + label(networkId, networkName)));
            }
            context.getLevel().sendBlockUpdated(pos, wire.getBlockState(), wire.getBlockState(), 3);
            return InteractionResult.SUCCESS;
        }
        player.sendSystemMessage(Component.literal("Use on a computer, router, dongle, or socket."));
        return InteractionResult.SUCCESS;
    }

    private static void storeNetwork(ItemStack stack, String networkId, String networkName) {
        CompoundTag tag = customTag(stack);
        tag.putString(NETWORK_ID, NetworkEndpointBlockEntity.sanitizeNetworkId(networkId));
        tag.putString(NETWORK_NAME, NetworkEndpointBlockEntity.sanitize(networkName));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static String storedNetworkId(ItemStack stack) {
        return NetworkEndpointBlockEntity.sanitizeNetworkId(customTag(stack).getStringOr(NETWORK_ID, ""));
    }

    private static String storedNetworkName(ItemStack stack) {
        return NetworkEndpointBlockEntity.sanitize(customTag(stack).getStringOr(NETWORK_NAME, ""));
    }

    private static CompoundTag customTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    private static String label(String networkId, String networkName) {
        return networkName == null || networkName.isBlank() ? networkId : networkName;
    }
}
