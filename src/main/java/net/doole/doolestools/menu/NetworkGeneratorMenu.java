package net.doole.doolestools.menu;

import net.doole.doolestools.blockentity.NetworkGeneratorBlockEntity;
import net.doole.doolestools.registry.ModBlocks;
import net.doole.doolestools.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetworkGeneratorMenu extends AbstractContainerMenu {
    private static final int GENERATOR_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final Level level;
    private final Container generator;
    private final DataSlot energy;
    private final DataSlot energyCapacity;
    private final DataSlot burnRemaining;
    private final DataSlot burnTime;
    private final DataSlot fePerTick;

    public NetworkGeneratorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new SimpleContainer(1));
    }

    public NetworkGeneratorMenu(int containerId, Inventory playerInventory, BlockPos pos, Container generator) {
        super(ModMenus.NETWORK_GENERATOR.get(), containerId);
        this.pos = pos;
        this.level = playerInventory.player.level();
        this.access = ContainerLevelAccess.create(level, pos);
        this.generator = generator;

        addSlot(new Slot(generator, 0, 19, 31) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return generator.canPlaceItem(0, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        NetworkGeneratorBlockEntity be = generator instanceof NetworkGeneratorBlockEntity blockEntity ? blockEntity : null;
        energy = addDataSlot(dataSlot(be, 0));
        energyCapacity = addDataSlot(dataSlot(be, 1));
        burnRemaining = addDataSlot(dataSlot(be, 2));
        burnTime = addDataSlot(dataSlot(be, 3));
        fePerTick = addDataSlot(dataSlot(be, 4));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int energy() {
        return energy.get();
    }

    public int energyCapacity() {
        return Math.max(1, energyCapacity.get());
    }

    public int burnRemaining() {
        return burnRemaining.get();
    }

    public int burnTime() {
        return burnTime.get();
    }

    public int fePerTick() {
        return fePerTick.get();
    }

    public int burnPercent() {
        return burnTime() <= 0 ? 0 : Math.round((burnRemaining() * 100f) / burnTime());
    }

    public int energyPercent() {
        return Math.round((energy() * 100f) / energyCapacity());
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModBlocks.NETWORK_GENERATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == GENERATOR_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (generator.canPlaceItem(0, stack)) {
            if (!moveItemStackTo(stack, GENERATOR_SLOT, GENERATOR_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    private static DataSlot dataSlot(NetworkGeneratorBlockEntity be, int index) {
        return new DataSlot() {
            private int clientValue;

            @Override
            public int get() {
                if (be == null) return clientValue;
                return switch (index) {
                    case 0 -> be.energy();
                    case 1 -> be.energyCapacity();
                    case 2 -> be.burnRemaining();
                    case 3 -> be.burnTime();
                    case 4 -> be.generatorFePerTick();
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
