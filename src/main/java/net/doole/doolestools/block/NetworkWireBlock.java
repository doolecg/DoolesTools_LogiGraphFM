package net.doole.doolestools.block;

import com.mojang.serialization.MapCodec;
import net.doole.doolestools.blockentity.NetworkEndpointBlockEntity;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class NetworkWireBlock extends Block implements EntityBlock {
    public static final MapCodec<NetworkWireBlock> CODEC = simpleCodec(NetworkWireBlock::new);
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty ENDPOINT_DOWN = BooleanProperty.create("endpoint_down");
    public static final BooleanProperty ENDPOINT_UP = BooleanProperty.create("endpoint_up");
    public static final BooleanProperty ENDPOINT_NORTH = BooleanProperty.create("endpoint_north");
    public static final BooleanProperty ENDPOINT_SOUTH = BooleanProperty.create("endpoint_south");
    public static final BooleanProperty ENDPOINT_WEST = BooleanProperty.create("endpoint_west");
    public static final BooleanProperty ENDPOINT_EAST = BooleanProperty.create("endpoint_east");
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape DOWN_SHAPE = Block.box(6, 0, 6, 10, 6, 10);
    private static final VoxelShape UP_SHAPE = Block.box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape NORTH_SHAPE = Block.box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SOUTH_SHAPE = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape EAST_SHAPE = Block.box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape ENDPOINT_DOWN_SHAPE = Block.box(3, 0, 3, 13, 2, 13);
    private static final VoxelShape ENDPOINT_UP_SHAPE = Block.box(3, 14, 3, 13, 16, 13);
    private static final VoxelShape ENDPOINT_NORTH_SHAPE = Block.box(3, 3, 0, 13, 13, 2);
    private static final VoxelShape ENDPOINT_SOUTH_SHAPE = Block.box(3, 3, 14, 13, 13, 16);
    private static final VoxelShape ENDPOINT_WEST_SHAPE = Block.box(0, 3, 3, 2, 13, 13);
    private static final VoxelShape ENDPOINT_EAST_SHAPE = Block.box(14, 3, 3, 16, 13, 13);

    public NetworkWireBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(DOWN, false).setValue(UP, false)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(EAST, false)
                .setValue(ENDPOINT_DOWN, false).setValue(ENDPOINT_UP, false)
                .setValue(ENDPOINT_NORTH, false).setValue(ENDPOINT_SOUTH, false)
                .setValue(ENDPOINT_WEST, false).setValue(ENDPOINT_EAST, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST,
                ENDPOINT_DOWN, ENDPOINT_UP, ENDPOINT_NORTH, ENDPOINT_SOUTH, ENDPOINT_WEST, ENDPOINT_EAST);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkWireBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof NetworkWireBlockEntity wire) wire.refreshConnections();
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Upgrades/screwdriver work on a plain right-click (no shift): vanilla skips useItemOn when the
        // player sneaks with an item in hand, so a shift-gated install could never fire.
        if (hasEndpointFlag(state) && level.getBlockEntity(pos) instanceof NetworkWireBlockEntity wire) {
            if (stack.getItem() == ModItems.NETWORK_SCREWDRIVER.get()) {
                if (!level.isClientSide()) removeOneUpgrade(level, pos, player, wire);
                return InteractionResult.SUCCESS;
            }
            String upgradeType = ModItems.upgradeType(stack);
            if (!upgradeType.isBlank()) {
                if (!level.isClientSide() && wire.installUpgrade(upgradeType)) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    level.sendBlockUpdated(pos, state, state, 3);
                }
                return InteractionResult.SUCCESS;
            }
        }

        // Placing a socket item onto a bare wire.
        String kind = null;
        if (stack.getItem() == ModItems.NETWORK_MODEM.get()) kind = "modem";
        if (kind != null) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NetworkWireBlockEntity wire) {
                Direction face = endpointInstallFace(level, pos, hit.getDirection());
                if (face != null && wire.installEndpoint(kind, face, "")) {
                    level.sendBlockUpdated(pos, state, wire.getBlockState(), 3);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Otherwise open the endpoint screen if one is mounted.
        if (hasEndpointFlag(state)) {
            if (level.isClientSide()) openWireEndpointScreen(level, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!hasEndpointFlag(state)) return InteractionResult.PASS;
        if (level.isClientSide()) openWireEndpointScreen(level, pos);
        return InteractionResult.SUCCESS;
    }

    private static void openWireEndpointScreen(Level level, BlockPos pos) {
        String title = "Network Endpoint";
        String name = "";
        String id = "0000";
        int[] upgrades = new int[] { 0, 0, 0, 0 };
        if (level.getBlockEntity(pos) instanceof NetworkWireBlockEntity wire) {
            title = wire.hasRouter() ? "Wireless Router" : wire.hasModem() ? "Cable Socket" : title;
            name = wire.hasEndpoint() ? wire.endpointName() : "";
            id = wire.hasEndpoint() ? wire.formattedEndpointId() : id;
            upgrades = wire.hasEndpoint() ? wire.upgradeCounts() : upgrades;
        }
        NetworkEndpointBlock.openEndpointNameScreen(pos, title, name, id, upgrades);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof NetworkWireBlockEntity wire && wire.hasEndpoint()) {
            if (!level.isClientSide()) {
                ItemStack drop = new ItemStack(ModItems.NETWORK_MODEM.get());
                if (player == null || !player.getAbilities().instabuild) popResource(level, pos, drop);
                wire.clearEndpoint();
            }
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
    }

    private static void removeOneUpgrade(Level level, BlockPos pos, Player player, NetworkWireBlockEntity wire) {
        for (String type : new String[] { "efficiency", "range", "stack", "speed" }) {
            if (!wire.removeUpgrade(type)) continue;
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(propertyFor(direction))) shape = Shapes.or(shape, shapeFor(direction));
            if (state.getValue(endpointPropertyFor(direction))) shape = Shapes.or(shape, endpointShapeFor(direction));
        }
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static VoxelShape shapeFor(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }

    private static VoxelShape endpointShapeFor(Direction direction) {
        return switch (direction) {
            case DOWN -> ENDPOINT_DOWN_SHAPE;
            case UP -> ENDPOINT_UP_SHAPE;
            case NORTH -> ENDPOINT_NORTH_SHAPE;
            case SOUTH -> ENDPOINT_SOUTH_SHAPE;
            case WEST -> ENDPOINT_WEST_SHAPE;
            case EAST -> ENDPOINT_EAST_SHAPE;
        };
    }

    public static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(propertyFor(direction), connectsTo(level, pos.relative(direction)));
            state = state.setValue(endpointPropertyFor(direction), false);
        }
        if (level.getBlockEntity(pos) instanceof NetworkWireBlockEntity wire && wire.hasEndpoint()) {
            state = state.setValue(endpointPropertyFor(wire.endpointFace()), true);
        }
        return state;
    }

    public static boolean connectsTo(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof NetworkWireBlockEntity || be instanceof net.doole.doolestools.blockentity.NetworkModemBlockEntity;
    }

    @Nullable
    public static Direction endpointInstallFace(BlockGetter level, BlockPos pos, Direction clickedFace) {
        if (isEndpointTarget(level, pos.relative(clickedFace))) return clickedFace;
        Direction found = null;
        for (Direction direction : Direction.values()) {
            if (!isEndpointTarget(level, pos.relative(direction))) continue;
            if (found != null) return null;
            found = direction;
        }
        return found == null ? clickedFace : found;
    }

    private static boolean isEndpointTarget(BlockGetter level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) return false;
        BlockEntity be = level.getBlockEntity(pos);
        return !(be instanceof NetworkWireBlockEntity) && !(be instanceof NetworkEndpointBlockEntity);
    }

    private static boolean hasEndpointFlag(BlockState state) {
        return state.getValue(ENDPOINT_DOWN) || state.getValue(ENDPOINT_UP)
                || state.getValue(ENDPOINT_NORTH) || state.getValue(ENDPOINT_SOUTH)
                || state.getValue(ENDPOINT_WEST) || state.getValue(ENDPOINT_EAST);
    }

    private static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    private static BooleanProperty endpointPropertyFor(Direction direction) {
        return switch (direction) {
            case DOWN -> ENDPOINT_DOWN;
            case UP -> ENDPOINT_UP;
            case NORTH -> ENDPOINT_NORTH;
            case SOUTH -> ENDPOINT_SOUTH;
            case WEST -> ENDPOINT_WEST;
            case EAST -> ENDPOINT_EAST;
        };
    }
}
