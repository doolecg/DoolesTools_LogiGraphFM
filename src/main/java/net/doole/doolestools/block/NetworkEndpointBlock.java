package net.doole.doolestools.block;

import com.mojang.serialization.MapCodec;
import net.doole.doolestools.blockentity.NetworkEndpointBlockEntity;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.item.UpgradeType;
import net.doole.doolestools.registry.ModItems;
import net.doole.doolestools.util.NetworkDismantle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public abstract class NetworkEndpointBlock extends DirectionalBlock implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WIRE_CONNECTED = BooleanProperty.create("wire_connected");

    // Plain pad shapes (no wire hub)
    private static final VoxelShape DOWN_SHAPE  = Block.box(3, 0,  3, 13,  2, 13);
    private static final VoxelShape UP_SHAPE    = Block.box(3, 14, 3, 13, 16, 13);
    private static final VoxelShape NORTH_SHAPE = Block.box(3, 3,  0, 13, 13,  2);
    private static final VoxelShape SOUTH_SHAPE = Block.box(3, 3, 14, 13, 13, 16);
    private static final VoxelShape WEST_SHAPE  = Block.box(0, 3,  3,  2, 13, 13);
    private static final VoxelShape EAST_SHAPE  = Block.box(14, 3, 3, 16, 13, 13);

    // Connected shapes: pad + 4×4×4 hub knob pointing away from the attached surface (toward the wire)
    private static final VoxelShape DOWN_CONNECTED  = Shapes.or(DOWN_SHAPE,  Block.box(6,  2, 6, 10,  6, 10));
    private static final VoxelShape UP_CONNECTED    = Shapes.or(UP_SHAPE,    Block.box(6, 10, 6, 10, 14, 10));
    private static final VoxelShape NORTH_CONNECTED = Shapes.or(NORTH_SHAPE, Block.box(6,  6, 2, 10, 10,  6));
    private static final VoxelShape SOUTH_CONNECTED = Shapes.or(SOUTH_SHAPE, Block.box(6,  6, 10, 10, 10, 14));
    private static final VoxelShape WEST_CONNECTED  = Shapes.or(WEST_SHAPE,  Block.box(2,  6, 6,  6, 10, 10));
    private static final VoxelShape EAST_CONNECTED  = Shapes.or(EAST_SHAPE,  Block.box(10, 6, 6, 14, 10, 10));

    protected NetworkEndpointBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WIRE_CONNECTED, false));
    }

    @Override
    protected abstract MapCodec<? extends NetworkEndpointBlock> codec();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WIRE_CONNECTED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(WIRE_CONNECTED, !(this instanceof NetworkModemBlock) && hasAdjacentWire(context.getLevel(), pos));
        return state.canSurvive(context.getLevel(), pos) ? state : null;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
            Block neighborBlock, net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) {
            boolean wired = !(this instanceof NetworkModemBlock) && hasAdjacentWire(level, pos);
            if (state.getValue(WIRE_CONNECTED) != wired)
                level.setBlock(pos, state.setValue(WIRE_CONNECTED, wired), 3);
        }
    }

    public static BlockPos attachedPos(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (level.getBlockEntity(pos) instanceof NetworkEndpointBlockEntity endpoint && endpoint.networkId().isBlank()) {
            String networkId = NetworkEndpointBlockEntity.inferNearbyNetwork(serverLevel, pos);
            if (!networkId.isBlank()) {
                endpoint.setNetworkId(networkId);
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos attached = attachedPos(pos, state);
        return !level.getBlockState(attached).isAir();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean connected = state.getValue(WIRE_CONNECTED);
        // getShape uses FACING.getOpposite() to select the side the pad sits on
        return switch (state.getValue(FACING).getOpposite()) {
            case DOWN  -> connected ? DOWN_CONNECTED  : DOWN_SHAPE;
            case UP    -> connected ? UP_CONNECTED    : UP_SHAPE;
            case NORTH -> connected ? NORTH_CONNECTED : NORTH_SHAPE;
            case SOUTH -> connected ? SOUTH_CONNECTED : SOUTH_SHAPE;
            case WEST  -> connected ? WEST_CONNECTED  : WEST_SHAPE;
            case EAST  -> connected ? EAST_CONNECTED  : EAST_SHAPE;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    // Note: we deliberately do NOT gate on sneaking. Vanilla skips useItemOn entirely when the player
    // is sneaking with an item in hand, so a shift-gated install can never fire. Plain right-click with
    // a card installs; plain right-click with the screwdriver removes; anything else opens the screen.
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (NetworkDismantle.tryDismantle(level, pos, player, stack)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof NetworkEndpointBlockEntity endpoint)) return InteractionResult.PASS;
        if (stack.getItem() == ModItems.NETWORK_SCREWDRIVER.get()) {
            if (!level.isClientSide()) removeOneUpgrade(level, pos, player, endpoint);
            return InteractionResult.SUCCESS;
        }
        UpgradeType upgradeType = ModItems.upgradeType(stack);
        if (upgradeType != null) {
            if (!level.isClientSide() && endpoint.installUpgrade(upgradeType)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.sendBlockUpdated(pos, state, state, 3);
                player.sendSystemMessage(Component.literal(upgradeType.label + " upgrade installed ("
                        + endpoint.upgradeCount(upgradeType) + "/" + NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE + ")"));
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            openEndpointNameScreen(pos, endpoint.deviceKind(), endpoint.deviceName(), endpoint.formattedDeviceId(), endpoint.upgradeCounts());
        }
        return InteractionResult.SUCCESS;
    }

    private static void removeOneUpgrade(Level level, BlockPos pos, Player player, NetworkEndpointBlockEntity endpoint) {
        for (UpgradeType type : UpgradeType.values()) {
            if (!endpoint.removeUpgrade(type)) continue;
            giveOrDrop(level, pos, player, type);
            player.sendSystemMessage(Component.literal(type.label + " upgrade removed"));
            return;
        }
        player.sendSystemMessage(Component.literal("No upgrades installed"));
    }

    private static void giveOrDrop(Level level, BlockPos pos, Player player, UpgradeType type) {
        Item item = ModItems.upgradeCard(type);
        if (item == null) return;
        ItemStack removed = new ItemStack(item);
        if (!player.addItem(removed)) popResource(level, pos, removed);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof NetworkEndpointBlockEntity endpoint) {
            openEndpointNameScreen(pos, endpoint.deviceKind(), endpoint.deviceName(), endpoint.formattedDeviceId(), endpoint.upgradeCounts());
        }
        return InteractionResult.SUCCESS;
    }

    static void openEndpointNameScreen(BlockPos pos, String title, String currentName, String currentId, int[] upgradeCounts) {
        try {
            Class<?> bridge = Class.forName("net.doole.doolestools.client.LabelGunClientBridge");
            Method open = bridge.getMethod("openEndpointName", BlockPos.class, String.class, String.class, String.class, int[].class);
            open.invoke(null, pos, title, currentName, currentId, upgradeCounts);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    static void openEndpointNameScreen(BlockPos pos, String title, String currentName, String currentId, int[] upgradeCounts, Direction face) {
        try {
            Class<?> bridge = Class.forName("net.doole.doolestools.client.LabelGunClientBridge");
            Method open = bridge.getMethod("openEndpointNameForFace", BlockPos.class, String.class, String.class, String.class, int[].class, Direction.class);
            open.invoke(null, pos, title, currentName, currentId, upgradeCounts, face);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (!tickState.canSurvive(tickLevel, pos)) tickLevel.destroyBlock(pos, true);
        };
    }

    private static boolean hasAdjacentWire(BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.values())
            if (level.getBlockEntity(pos.relative(d)) instanceof NetworkWireBlockEntity)
                return true;
        return false;
    }
}
