package net.doole.doolestools.blockentity;

import net.doole.doolestools.block.LogiGraphWallMonitorBlock;
import net.doole.doolestools.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class LogiGraphWallMonitorBlockEntity extends BlockEntity {
    private BlockPos controller = BlockPos.ZERO;
    private int width = 1;
    private int height = 1;
    private int tileX;
    private int tileY;
    private boolean valid = true;
    private BlockPos linkedComputer = BlockPos.ZERO;
    private int mode;
    private int recalcCooldown;

    public LogiGraphWallMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGIGRAPH_WALL_MONITOR.get(), pos, state);
        this.controller = pos;
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        if (recalcCooldown-- <= 0) {
            recalcCooldown = 20;
            recalculateMultiblock(level, worldPosition);
        }
    }

    public boolean valid() {
        return valid;
    }

    public BlockPos controller() {
        return controller;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int tileX() {
        return tileX;
    }

    public int tileY() {
        return tileY;
    }

    public BlockPos linkedComputer() {
        return linkedComputer;
    }

    public int mode() {
        return mode;
    }

    public static final int MODE_COUNT = 6;

    /** Cycles the display mode on the controller of this multiblock. */
    public void cycleMode() {
        LogiGraphWallMonitorBlockEntity ctrl = controllerEntity();
        ctrl.mode = (ctrl.mode + 1) % MODE_COUNT;
        ctrl.setChanged();
        ctrl.pushUpdate();
    }

    /** Links the controller to the nearest Logistics Computer within {@code radius}; returns true if one was found. */
    public boolean linkToNearestComputer(int radius) {
        if (level == null) return false;
        LogiGraphWallMonitorBlockEntity ctrl = controllerEntity();
        BlockPos origin = ctrl.getBlockPos();
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (!(level.getBlockEntity(pos) instanceof net.doole.doolestools.blockentity.LogisticsComputerBlockEntity)) continue;
            double sq = origin.distSqr(pos);
            if (sq < bestSq) {
                bestSq = sq;
                best = pos.immutable();
            }
        }
        if (best == null) return false;
        ctrl.linkedComputer = best;
        ctrl.setChanged();
        ctrl.pushUpdate();
        return true;
    }

    public void unlink() {
        LogiGraphWallMonitorBlockEntity ctrl = controllerEntity();
        ctrl.linkedComputer = BlockPos.ZERO;
        ctrl.setChanged();
        ctrl.pushUpdate();
    }

    private LogiGraphWallMonitorBlockEntity controllerEntity() {
        if (level != null && !controller.equals(worldPosition)
                && level.getBlockEntity(controller) instanceof LogiGraphWallMonitorBlockEntity ctrl) {
            return ctrl;
        }
        return this;
    }

    private void pushUpdate() {
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void recalculateMultiblock(Level level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (!(originState.getBlock() instanceof LogiGraphWallMonitorBlock)) return;
        Direction facing = originState.getValue(LogiGraphWallMonitorBlock.FACING);
        Direction horizontal = facing.getClockWise();
        Direction vertical = Direction.UP;

        Set<BlockPos> connected = collectConnected(level, origin, facing, horizontal, vertical);
        Bounds bounds = bounds(connected, origin, horizontal, vertical);
        boolean rectangleValid = connected.size() == bounds.width * bounds.height;
        for (int x = 0; x < bounds.width && rectangleValid; x++) {
            for (int y = 0; y < bounds.height; y++) {
                BlockPos pos = bounds.min.offset(horizontal.getStepX() * x + vertical.getStepX() * y,
                        horizontal.getStepY() * x + vertical.getStepY() * y,
                        horizontal.getStepZ() * x + vertical.getStepZ() * y);
                if (!connected.contains(pos)) {
                    rectangleValid = false;
                    break;
                }
            }
        }

        BlockPos controllerPos = bounds.min;
        for (BlockPos pos : connected) {
            if (level.getBlockEntity(pos) instanceof LogiGraphWallMonitorBlockEntity monitor) {
                int x = coordinateDelta(bounds.min, pos, horizontal);
                int y = coordinateDelta(bounds.min, pos, vertical);
                monitor.applyMultiblock(controllerPos, bounds.width, bounds.height, x, y, rectangleValid);
            }
        }
    }

    private Set<BlockPos> collectConnected(Level level, BlockPos origin, Direction facing, Direction horizontal, Direction vertical) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) continue;
            for (Direction direction : new Direction[]{horizontal, horizontal.getOpposite(), vertical, vertical.getOpposite()}) {
                BlockPos next = pos.relative(direction);
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof LogiGraphWallMonitorBlock
                        && state.getValue(LogiGraphWallMonitorBlock.FACING) == facing
                        && !visited.contains(next)) {
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    private void applyMultiblock(BlockPos controller, int width, int height, int tileX, int tileY, boolean valid) {
        boolean changed = !this.controller.equals(controller) || this.width != width || this.height != height
                || this.tileX != tileX || this.tileY != tileY || this.valid != valid;
        this.controller = controller;
        this.width = width;
        this.height = height;
        this.tileX = tileX;
        this.tileY = tileY;
        this.valid = valid;
        if (changed) setChanged();
        if (changed && level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private Bounds bounds(Set<BlockPos> positions, BlockPos origin, Direction horizontal, Direction vertical) {
        int minX = 0, maxX = 0, minY = 0, maxY = 0;
        for (BlockPos pos : positions) {
            int x = coordinateDelta(origin, pos, horizontal);
            int y = coordinateDelta(origin, pos, vertical);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        BlockPos min = origin.offset(horizontal.getStepX() * minX + vertical.getStepX() * minY,
                horizontal.getStepY() * minX + vertical.getStepY() * minY,
                horizontal.getStepZ() * minX + vertical.getStepZ() * minY);
        return new Bounds(min, maxX - minX + 1, maxY - minY + 1);
    }

    private static int coordinateDelta(BlockPos origin, BlockPos pos, Direction axis) {
        return (pos.getX() - origin.getX()) * axis.getStepX()
                + (pos.getY() - origin.getY()) * axis.getStepY()
                + (pos.getZ() - origin.getZ()) * axis.getStepZ();
    }

    private record Bounds(BlockPos min, int width, int height) {}

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("controllerX", controller.getX());
        output.putInt("controllerY", controller.getY());
        output.putInt("controllerZ", controller.getZ());
        output.putInt("width", width);
        output.putInt("height", height);
        output.putInt("tileX", tileX);
        output.putInt("tileY", tileY);
        output.putBoolean("valid", valid);
        output.putInt("linkedComputerX", linkedComputer.getX());
        output.putInt("linkedComputerY", linkedComputer.getY());
        output.putInt("linkedComputerZ", linkedComputer.getZ());
        output.putInt("mode", mode);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.controller = new BlockPos(input.getIntOr("controllerX", worldPosition.getX()), input.getIntOr("controllerY", worldPosition.getY()), input.getIntOr("controllerZ", worldPosition.getZ()));
        this.width = input.getIntOr("width", 1);
        this.height = input.getIntOr("height", 1);
        this.tileX = input.getIntOr("tileX", 0);
        this.tileY = input.getIntOr("tileY", 0);
        this.valid = input.getBooleanOr("valid", true);
        this.linkedComputer = new BlockPos(input.getIntOr("linkedComputerX", 0), input.getIntOr("linkedComputerY", 0), input.getIntOr("linkedComputerZ", 0));
        this.mode = input.getIntOr("mode", 0);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        saveAdditional(output);
        return output.buildResult();
    }
}
