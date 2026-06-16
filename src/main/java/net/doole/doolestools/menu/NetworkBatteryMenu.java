package net.doole.doolestools.menu;

import net.doole.doolestools.blockentity.NetworkBatteryBlockEntity;
import net.doole.doolestools.registry.ModBlocks;
import net.doole.doolestools.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NetworkBatteryMenu extends AbstractContainerMenu {
    private static final int INV_END = 27;
    private static final int HOTBAR_END = 36;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final DataSlot energy;
    private final DataSlot energyPercent;

    public NetworkBatteryMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null);
    }

    public NetworkBatteryMenu(int containerId, Inventory playerInventory, BlockPos pos, NetworkBatteryBlockEntity be) {
        super(ModMenus.NETWORK_BATTERY.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        energy = addDataSlot(dataSlot(be, 0));
        energyPercent = addDataSlot(dataSlot(be, 1));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int energy() {
        return energy.get();
    }

    public int energyPercent() {
        return energyPercent.get();
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModBlocks.NETWORK_BATTERY.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        // No machine slots — just shuffle between the inventory grid and the hotbar.
        if (index < INV_END) {
            if (!moveItemStackTo(stack, INV_END, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, INV_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    private static DataSlot dataSlot(NetworkBatteryBlockEntity be, int index) {
        return new DataSlot() {
            private int clientValue;

            @Override
            public int get() {
                if (be == null) return clientValue;
                return switch (index) {
                    case 0 -> be.energy();
                    case 1 -> be.energyPercent();
                    default -> 0;
                };
            }

            @Override
            public void set(int value) {
                clientValue = value;
            }
        };
    }
}
