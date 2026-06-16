package net.doole.doolestools.blockentity;

import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.menu.NetworkGeneratorMenu;
import net.doole.doolestools.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class NetworkGeneratorBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final int ENERGY_CAPACITY = 100_000;
    private static final int MAX_FUEL_STACK = 64;

    private ItemStack fuel = ItemStack.EMPTY;
    private int energy;
    private int burnRemaining;
    private int burnTime;

    private final SnapshotJournal<Snapshot> journal = new SnapshotJournal<>() {
        @Override
        protected Snapshot createSnapshot() {
            return new Snapshot(fuel.copy(), energy, burnRemaining, burnTime);
        }

        @Override
        protected void revertToSnapshot(Snapshot snapshot) {
            fuel = snapshot.fuel().copy();
            energy = snapshot.energy();
            burnRemaining = snapshot.burnRemaining();
            burnTime = snapshot.burnTime();
        }

        @Override
        protected void onRootCommit(Snapshot snapshot) {
            setChanged();
        }
    };

    private final EnergyHandler energyHandler = new GeneratorEnergyHandler();
    private final ResourceHandler<ItemResource> itemHandler = new FuelItemHandler();

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

    public EnergyHandler energyHandler() {
        return energyHandler;
    }

    public ResourceHandler<ItemResource> itemHandler() {
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
        return level == null ? 0 : level.fuelValues().burnDuration(stack);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("fuel", ItemStack.OPTIONAL_CODEC, fuel);
        output.putInt("energy", energy);
        output.putInt("burnRemaining", burnRemaining);
        output.putInt("burnTime", burnTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel = input.read("fuel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        energy = Math.max(0, Math.min(ENERGY_CAPACITY, input.getIntOr("energy", 0)));
        burnRemaining = Math.max(0, input.getIntOr("burnRemaining", 0));
        burnTime = Math.max(0, input.getIntOr("burnTime", 0));
    }

    private record Snapshot(ItemStack fuel, int energy, int burnRemaining, int burnTime) {}

    private final class GeneratorEnergyHandler implements EnergyHandler {
        @Override
        public long getAmountAsLong() {
            return energy;
        }

        @Override
        public long getCapacityAsLong() {
            return ENERGY_CAPACITY;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int extracted = Math.min(Math.max(0, amount), energy);
            if (extracted <= 0) return 0;
            journal.updateSnapshots(transaction);
            energy -= extracted;
            return extracted;
        }
    }

    private final class FuelItemHandler implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int slot) {
            return slot == 0 && !fuel.isEmpty() ? ItemResource.of(fuel) : ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int slot) {
            return slot == 0 ? fuel.getCount() : 0;
        }

        @Override
        public long getCapacityAsLong(int slot, ItemResource resource) {
            return slot == 0 && isValid(slot, resource) ? MAX_FUEL_STACK : 0;
        }

        @Override
        public boolean isValid(int slot, ItemResource resource) {
            return slot == 0 && resource != null && !resource.isEmpty() && isFuel(resource.toStack());
        }

        @Override
        public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            if (!isValid(slot, resource) || amount <= 0) return 0;
            if (!fuel.isEmpty() && !resource.matches(fuel)) return 0;
            int inserted = Math.min(amount, MAX_FUEL_STACK - fuel.getCount());
            if (inserted <= 0) return 0;
            journal.updateSnapshots(transaction);
            if (fuel.isEmpty()) fuel = resource.toStack(inserted);
            else fuel.grow(inserted);
            return inserted;
        }

        @Override
        public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
