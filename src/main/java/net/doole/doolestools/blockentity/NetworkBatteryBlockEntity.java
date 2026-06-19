package net.doole.doolestools.blockentity;

import net.doole.doolestools.menu.NetworkBatteryMenu;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * A network FE buffer: accepts energy pushed in by external sources and lets the computer (or anything
 * adjacent) pull it back out, smoothing supply between a Network Generator and a hungry network.
 */
public class NetworkBatteryBlockEntity extends BlockEntity implements MenuProvider {
    private static final int CAPACITY = 4_000_000;
    private static final int MAX_IO = 20_000;

    private int energy;

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accepted = Math.min(Math.min(Math.max(0, amount), MAX_IO), CAPACITY - energy);
            if (accepted <= 0) return 0;
            if (!simulate) {
                energy += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int extracted = Math.min(Math.min(Math.max(0, amount), MAX_IO), energy);
            if (extracted <= 0) return 0;
            if (!simulate) {
                energy -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return CAPACITY;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public NetworkBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_BATTERY.get(), pos, state);
    }

    public IEnergyStorage energyStorage() {
        return energyStorage;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveData(new ValueOutput(tag, registries));
    }

    private void saveData(ValueOutput output) {
        output.putInt("energy", energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadData(new ValueInput(tag, registries));
    }

    private void loadData(ValueInput input) {
        energy = Math.max(0, Math.min(CAPACITY, input.getIntOr("energy", 0)));
    }
}
