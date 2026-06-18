package net.doole.doolestools.block;

import com.mojang.serialization.MapCodec;
import net.doole.doolestools.blockentity.LogisticsComputerBlockEntity;
import net.doole.doolestools.blockentity.NetworkSwitchboardBlockEntity;
import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.util.NetworkDismantle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** The Logistics Computer: scans the area and hosts the flowgraph editor GUI. */
public class LogisticsComputerBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<LogisticsComputerBlock> CODEC = simpleCodec(LogisticsComputerBlock::new);

    public LogisticsComputerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsComputerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (tickLevel instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && blockEntity instanceof LogisticsComputerBlockEntity computer) {
                computer.serverTick(serverLevel);
            }
        };
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return ModServerConfig.REDSTONE_ALERT_ON_ERROR.get();
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel
                && serverLevel.getBlockEntity(pos) instanceof LogisticsComputerBlockEntity computer) {
            NetworkSwitchboardBlockEntity.removeNetworkFromLoaded(serverLevel, computer.networkId());
        }
        super.destroy(level, pos, state);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!ModServerConfig.REDSTONE_ALERT_ON_ERROR.get()) return 0;
        if (level.getBlockEntity(pos) instanceof LogisticsComputerBlockEntity be) {
            return be.hasErrorWarnings() ? 15 : 0;
        }
        return 0;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (NetworkDismantle.tryDismantle(level, pos, player, stack)) return InteractionResult.SUCCESS;
        return open(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }

    private static InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof LogisticsComputerBlockEntity be) {
                serverPlayer.openMenu(be, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
