package net.doole.doolestools.menu;

import net.doole.doolestools.blockentity.LogisticsComputerBlockEntity;
import net.doole.doolestools.registry.ModBlocks;
import net.doole.doolestools.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Slotless menu for the Logistics Computer. The GUI itself is fully custom-rendered; this menu
 * exists so the server tracks the open screen, validates range, and routes payloads to the BE.
 */
public class LogisticsComputerMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final Level level;

    /** Client constructor (reads the BE position from the open-screen buffer). */
    public LogisticsComputerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    /** Server constructor. */
    public LogisticsComputerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenus.LOGISTICS_COMPUTER.get(), containerId);
        this.pos = pos;
        this.level = playerInventory.player.level();
        this.access = ContainerLevelAccess.create(level, pos);
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public LogisticsComputerBlockEntity getBlockEntity() {
        if (level.getBlockEntity(pos) instanceof LogisticsComputerBlockEntity be) {
            return be;
        }
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModBlocks.LOGISTICS_COMPUTER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No slots in this menu.
        return ItemStack.EMPTY;
    }
}
