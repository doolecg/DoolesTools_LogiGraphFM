package net.doole.doolestools.block;

import com.mojang.serialization.MapCodec;
import net.doole.doolestools.blockentity.NetworkRelayBlockEntity;
import net.doole.doolestools.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class NetworkRelayBlock extends Block implements EntityBlock {
    public static final MapCodec<NetworkRelayBlock> CODEC = simpleCodec(NetworkRelayBlock::new);

    public NetworkRelayBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkRelayBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof NetworkRelayBlockEntity relay)) return InteractionResult.PASS;
        if (stack.getItem() == ModItems.NETWORK_SCREWDRIVER.get()) {
            if (!level.isClientSide()) removeOneUpgrade(level, pos, player, relay);
            return InteractionResult.SUCCESS;
        }
        String upgradeType = ModItems.upgradeType(stack);
        if (!upgradeType.isBlank()) {
            if (!level.isClientSide() && relay.installUpgrade(upgradeType)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                player.sendSystemMessage(Component.literal("Relay " + upgradeType + " upgrade installed (" + relay.upgradeCount(upgradeType) + "/" + NetworkRelayBlockEntity.MAX_UPGRADES_PER_TYPE + ")"));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void removeOneUpgrade(Level level, BlockPos pos, Player player, NetworkRelayBlockEntity relay) {
        for (String type : new String[] { "efficiency", "range", "speed" }) {
            if (!relay.removeUpgrade(type)) continue;
            giveOrDrop(level, pos, player, type);
            player.sendSystemMessage(Component.literal(ModItems.upgradeLabel(type) + " upgrade removed"));
            return;
        }
        player.sendSystemMessage(Component.literal("No upgrades installed"));
    }

    private static void giveOrDrop(Level level, BlockPos pos, Player player, String type) {
        Item item = ModItems.upgradeCard(type);
        if (item == null) return;
        ItemStack removed = new ItemStack(item);
        if (!player.addItem(removed)) popResource(level, pos, removed);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof NetworkRelayBlockEntity relay) {
            openClientScreen(pos, relay.displayName(), relay.formattedRelayId(), relay.upgradeCounts());
        } else if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof NetworkRelayBlockEntity relay) {
            serverPlayer.sendSystemMessage(Component.literal(relay.displayName() + " [" + relay.formattedRelayId() + "]"));
        }
        return InteractionResult.SUCCESS;
    }

    private static void openClientScreen(BlockPos pos, String currentName, String currentId, int[] upgradeCounts) {
        try {
            Class<?> bridge = Class.forName("net.doole.doolestools.client.LabelGunClientBridge");
            java.lang.reflect.Method open = bridge.getMethod("openEndpointName", BlockPos.class, String.class, String.class, String.class, int[].class);
            open.invoke(null, pos, "Network Relay", currentName, currentId, upgradeCounts);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
