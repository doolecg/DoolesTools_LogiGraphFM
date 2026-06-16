package net.doole.doolestools.block;

import com.mojang.serialization.MapCodec;
import net.doole.doolestools.blockentity.LogiGraphWallMonitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class LogiGraphWallMonitorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<LogiGraphWallMonitorBlock> CODEC = simpleCodec(LogiGraphWallMonitorBlock::new);

    public LogiGraphWallMonitorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogiGraphWallMonitorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LogiGraphWallMonitorBlockEntity monitor)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                // Sneak: link to the nearest computer, or unlink if already linked.
                if (!monitor.linkedComputer().equals(BlockPos.ZERO)) {
                    monitor.unlink();
                    player.sendSystemMessage(Component.translatable("message.doolestools.wall_monitor.unlinked"));
                } else if (monitor.linkToNearestComputer(16)) {
                    player.sendSystemMessage(Component.translatable("message.doolestools.wall_monitor.linked"));
                } else {
                    player.sendSystemMessage(Component.translatable("message.doolestools.wall_monitor.no_computer"));
                }
            } else {
                monitor.cycleMode();
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof LogiGraphWallMonitorBlockEntity monitor) monitor.serverTick();
        };
    }
}
