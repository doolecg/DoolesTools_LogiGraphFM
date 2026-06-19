package net.doole.doolestools.client.screen;

import net.doole.doolestools.client.gui.DUTheme;
import net.doole.doolestools.menu.NetworkBatteryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class NetworkBatteryScreen extends AbstractContainerScreen<NetworkBatteryMenu> {
    public NetworkBatteryScreen(NetworkBatteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        // Hide the default white title / "Inventory" labels; we draw our own green ones.
        this.titleLabelX = -9999;
        this.titleLabelY = -9999;
        this.inventoryLabelX = -9999;
        this.inventoryLabelY = -9999;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        DUTheme.bezel(g, leftPos, topPos, imageWidth, imageHeight);
        DUTheme.box(g, leftPos + 5, topPos + 5, imageWidth - 10, imageHeight - 10, DUTheme.SCREEN, DUTheme.PANEL_BORDER);
        DUTheme.box(g, leftPos + 10, topPos + 10, imageWidth - 20, 56, DUTheme.PANEL, DUTheme.PANEL_BORDER);

        g.drawString(font, "NETWORK BATTERY", leftPos + 14, topPos + 14, DUTheme.TEXT_GREEN, false);

        int pct = menu.energyPercent();
        g.drawString(font, "STORED", leftPos + 14, topPos + 28, DUTheme.TEXT_DIM, false);
        g.drawString(font, format(menu.energy()) + " FE", leftPos + 60, topPos + 28, DUTheme.TEXT, false);
        DUTheme.progress(g, leftPos + 14, topPos + 41, imageWidth - 44, 10, pct / 100f, DUTheme.PROGRESS_BLUE);
        g.drawString(font, pct + "%", leftPos + imageWidth - 26, topPos + 53, DUTheme.TEXT_DIM, false);

        g.drawString(font, "Inventory", leftPos + 8, topPos + 72, DUTheme.TEXT_DIM, false);

        // Vanilla-style slot wells behind every slot.
        for (Slot slot : menu.slots) {
            DUTheme.slotWell(g, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
    }

    private static String format(int fe) {
        if (fe >= 1_000_000) return String.format("%.1fM", fe / 1_000_000f);
        if (fe >= 1_000) return String.format("%.1fk", fe / 1_000f);
        return Integer.toString(fe);
    }
}
