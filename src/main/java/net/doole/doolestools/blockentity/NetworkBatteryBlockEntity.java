package net.doole.doolestools.blockentity;

import net.doole.doolestools.menu.NetworkBatteryMenu;
import net.doole.doolestools.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

/**
 * A network FE buffer: accepts energy pushed in by external sources and lets the computer (or anything
 * adjacent) pull it back out, smoothing supply between a Network Generator and a hungry network.
 */
public class NetworkBatteryBlockEntity extends BlockEntity implements MenuProvider {
    private static final int CAPACITY = 4_000_000;
    private static final int MAX_IO = 20_000;

    private int energy;

    private final SnapshotJournal<Integer> journal = new SnapshotJournal<>() {
        @Override
        protected Integer createSnapshot() {
            return energy;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            energy = snapshot;
        }

        @Override
        protected void onRootCommit(Integer snapshot) {
            setChanged();
        }
    };

    private final EnergyHandler energyHandler = new EnergyHandler() {
        @Override
        public long getAmountAsLong() {
            return energy;
        }

        @Override
        public long getCapacityAsLong() {
            return CAPACITY;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int accepted = Math.min(Math.min(Math.max(0, amount), MAX_IO), CAPACITY - energy);
            if (accepted <= 0) return 0;
            journal.updateSnapshots(transaction);
            energy += accepted;
            return accepted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int extracted = Math.min(Math.min(Math.max(0, amount), MAX_IO), energy);
            if (extracted <= 0) return 0;
            journal.updateSnapshots(transaction);
            energy -= extracted;
            return extracted;
        }
    };

    public NetworkBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_BATTERY.get(), pos, state);
    }

    public EnergyHandler energyHandler() {
        return energyHandler;
    }

    public int energy() {
        return energy;
    }

    public int capacity() {
        return CAPACITY;
    }

    public int energyPercent() {
        return (int) Math.round(energy * 100.0 / CAPACITY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.doolestools.network_battery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NetworkBatteryMenu(containerId, playerInventory, worldPosition, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energy);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = Math.max(0, Math.min(CAPACITY, input.getIntOr("energy", 0)));
    }
}
