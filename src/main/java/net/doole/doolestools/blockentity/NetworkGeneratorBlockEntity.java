package net.doole.doolestools.blockentity;

import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.menu.NetworkGeneratorMenu;
import net.doole.doolestools.registry.ModBlockEntities;
import net.doole.doolestools.util.ValueInput;
import net.doole.doolestools.util.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

public class NetworkGeneratorBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final int ENERGY_CAPACITY = 100_000;
    private static final int MAX_FUEL_STACK = 64;

    private ItemStack fuel = ItemStack.EMPTY;
    private int energy;
    private int burnRemaining;
    private int burnTime;

    private final IEnergyStorage energyStorage = new GeneratorEnergyStorage();
    private final IItemHandler itemHandler = new FuelItemHandler();

    public NetworkGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_GENERATOR.get(), pos, state);
    }

    public void serverTick(ServerLevel level) {
        boolean changed = false;
        if (burnRemaining <= 0 && energy < ENERGY_CAPACITY && isFuel(fuel)) {
            int duration = burnDuration(fuel);
            if (duration > 0) {
                burnTime = duration;
                burnRemaining = duration;
                fuel.shrink(1);
                if (fuel.isEmpty()) fuel = ItemStack.EMPTY;
                changed = true;
            }
        }
        if (burnRemaining > 0 && energy < ENERGY_CAPACITY) {
            burnRemaining--;
            energy = Math.min(ENERGY_CAPACITY, energy + ModServerConfig.GENERATOR_FE_PER_BURN_TICK.get());
            changed = true;
        }
        // Mark for save only. We deliberately do NOT call sendBlockUpdated here: the block has no
        // visual burn state and the GUI syncs energy/burn through the menu's DataSlots, so a per-tick
        // block update would just spam neighbour notifications and client packets (a real lag spike).
        if (changed) setChanged();
    }

    public IEnergyStorage energyStorage() {
        return energyStorage;
    }

    public IItemHandler itemHandler() {
        return itemHandler;
    }

    public int energy() {
        return energy;
    }

    public int energyCapacity() {
        return ENERGY_CAPACITY;
    }

    public int burnRemaining() {
        return burnRemaining;
    }

    public int burnTime() {
        return burnTime;
    }

    public int generatorFePerTick() {
        return ModServerConfig.GENERATOR_FE_PER_BURN_TICK.get();
    }

    public boolean insertFuelFromPlayer(ItemStack stack, boolean creative) {
        if (!isFuel(stack)) return false;
        if (!fuel.isEmpty() && !ItemStack.isSameItemSameComponents(fuel, stack)) return false;
        int room = MAX_FUEL_STACK - fuel.getCount();
        if (room <= 0) return false;
        if (fuel.isEmpty()) fuel = stack.copyWithCount(1);
        else fuel.grow(1);
        if (!creative) stack.shrink(1);
        setChanged();
        return true;
    }

    public boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && burnDuration(stack) > 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.doolestools.network_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NetworkGeneratorMenu(containerId, playerInventory, worldPosition, this);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return fuel.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? fuel : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0 || fuel.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = fuel.split(amount);
        if (fuel.isEmpty()) fuel = ItemStack.EMPTY;
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack removed = fuel;
        fuel = ItemStack.EMPTY;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        fuel = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(Math.min(MAX_FUEL_STACK, stack.getCount()));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && isFuel(stack);
    }

    @Override
    public void clearContent() {
        fuel = ItemStack.EMPTY;
        setChanged();
    }

    private int burnDuration(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveData(new ValueOutput(tag, registries));
    }

    private void saveData(ValueOutput output) {
        output.store("fuel", ItemStack.OPTIONAL_CODEC, fuel);
        output.putInt("energy", energy);
        output.putInt("burnRemaining", burnRemaining);
        output.putInt("burnTime", burnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadData(new ValueInput(tag, registries));
    }

    private void loadData(ValueInput input) {
        fuel = input.read("fuel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        energy = Math.max(0, Math.min(ENERGY_CAPACITY, input.getIntOr("energy", 0)));
        burnRemaining = Math.max(0, input.getIntOr("burnRemaining", 0));
        burnTime = Math.max(0, input.getIntOr("burnTime", 0));
    }

    private final class GeneratorEnergyStorage implements IEnergyStorage {
        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return ENERGY_CAPACITY;
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int extracted = Math.min(Math.max(0, amount), energy);
            if (extracted <= 0) return 0;
            if (!simulate) {
                energy -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public boolean canExtract() { return true; }

        @Override
        public boolean canReceive() { return false; }
    }

    private final class FuelItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? fuel.copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !isFuel(stack)) return stack;
            if (!fuel.isEmpty() && !ItemStack.isSameItemSameComponents(fuel, stack)) return stack;
            int inserted = Math.min(stack.getCount(), MAX_FUEL_STACK - fuel.getCount());
            if (inserted <= 0) return stack;
            if (!simulate) {
                if (fuel.isEmpty()) fuel = stack.copyWithCount(inserted);
                else fuel.grow(inserted);
                setChanged();
            }
            ItemStack remainder = stack.copy();
            remainder.shrink(inserted);
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? MAX_FUEL_STACK : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && isFuel(stack);
        }
    }
}
