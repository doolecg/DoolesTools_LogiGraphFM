package net.doole.doolestools.menu;

import net.doole.doolestools.blockentity.LogisticsMonitorBlockEntity;
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

/** Slotless menu for the read-only Logistics Monitor. */
public class LogisticsMonitorMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final Level level;

    public LogisticsMonitorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    public LogisticsMonitorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenus.LOGISTICS_MONITOR.get(), containerId);
        this.pos = pos;
        this.level = playerInventory.player.level();
        this.access = ContainerLevelAccess.create(level, pos);
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public LogisticsMonitorBlockEntity getBlockEntity() {
        if (level.getBlockEntity(pos) instanceof LogisticsMonitorBlockEntity be) {
            return be;
        }
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModBlocks.LOGISTICS_MONITOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
