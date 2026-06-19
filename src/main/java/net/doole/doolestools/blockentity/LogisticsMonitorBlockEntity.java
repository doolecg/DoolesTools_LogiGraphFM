package net.doole.doolestools.blockentity;

import com.mojang.serialization.Codec;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.menu.LogisticsMonitorMenu;
import net.doole.doolestools.registry.ModBlockEntities;
import net.doole.doolestools.util.ValueInput;
import net.doole.doolestools.util.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A wall-mounted monitor that mirrors the saved graph/warnings from a linked Logistics Computer.
 * Read-only in the MVP. Holds only a link to a computer position and the active display mode.
 */
public class LogisticsMonitorBlockEntity extends BlockEntity implements MenuProvider {

    public enum Mode {
        FLOWGRAPH, WARNINGS, STORAGE_SUMMARY;

        /** Codec used for persistence. Falls back to {@link #FLOWGRAPH} on unrecognised names. */
        public static final Codec<Mode> CODEC = Codec.STRING.xmap(
                s -> {
                    try { return Mode.valueOf(s); } catch (IllegalArgumentException e) { return FLOWGRAPH; }
                },
                Mode::name);

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Computers within this range can be auto-linked when the monitor is placed/used. */
    public static final int LINK_RANGE = 16;

    @Nullable
    private BlockPos linkedComputer;
    private Mode mode = Mode.FLOWGRAPH;

    public LogisticsMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGISTICS_MONITOR.get(), pos, state);
    }

    @Nullable
    public BlockPos getLinkedComputer() {
        return linkedComputer;
    }

    public void setLinkedComputer(@Nullable BlockPos pos) {
        this.linkedComputer = pos;
        setChanged();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        setChanged();
    }

    public void cycleMode() {
        setMode(mode.next());
    }

    /** Resolves the linked computer block entity if loaded, else null. */
    @Nullable
    public LogisticsComputerBlockEntity resolveComputer() {
        if (level == null || linkedComputer == null) return null;
        if (!level.hasChunkAt(linkedComputer)) return null;
        if (level.getBlockEntity(linkedComputer) instanceof LogisticsComputerBlockEntity computer) {
            return computer;
        }
        return null;
    }

    public LogisticsGraphData resolveGraph() {
        LogisticsComputerBlockEntity computer = resolveComputer();
        return computer == null ? LogisticsGraphData.EMPTY : computer.getGraph();
    }

    public List<ScannedBlockData> resolveScan() {
        LogisticsComputerBlockEntity computer = resolveComputer();
        return computer == null ? List.of() : computer.getLastScan();
    }

    /** Find the nearest computer within {@link #LINK_RANGE}. Used to auto-link on placement. */
    public void autoLink() {
        if (level == null) return;
        BlockPos found = null;
        double best = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(
                worldPosition.offset(-LINK_RANGE, -LINK_RANGE, -LINK_RANGE),
                worldPosition.offset(LINK_RANGE, LINK_RANGE, LINK_RANGE))) {
            if (!level.hasChunkAt(p)) continue;
            if (level.getBlockEntity(p) instanceof LogisticsComputerBlockEntity) {
                double d = p.distSqr(worldPosition);
                if (d < best) {
                    best = d;
                    found = p.immutable();
                }
            }
        }
        if (found != null) {
            setLinkedComputer(found);
        }
    }

    // --- Persistence ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveData(new ValueOutput(tag, registries));
    }

    private void saveData(ValueOutput output) {
        output.store("mode", Mode.CODEC, mode);
        output.storeNullable("linkedComputer", BlockPos.CODEC, linkedComputer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadData(new ValueInput(tag, registries));
    }

    private void loadData(ValueInput input) {
        this.mode = input.read("mode", Mode.CODEC).orElse(Mode.FLOWGRAPH);
        this.linkedComputer = input.read("linkedComputer", BlockPos.CODEC).orElse(null);
    }

    // --- Menu ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.doolestools.logistics_monitor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LogisticsMonitorMenu(containerId, playerInventory, this.worldPosition);
    }

    public Level levelOrNull() {
        return level;
    }
}
