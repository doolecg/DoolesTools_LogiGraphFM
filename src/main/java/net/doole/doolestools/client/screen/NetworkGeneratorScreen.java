package net.doole.doolestools.client.screen;

import net.doole.doolestools.client.gui.DUTheme;
import net.doole.doolestools.menu.NetworkGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class NetworkGeneratorScreen extends AbstractContainerScreen<NetworkGeneratorMenu> {
    public NetworkGeneratorScreen(NetworkGeneratorMenu menu, Inventory inventory, Component title) {
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

        g.drawString(font, "NETWORK GENERATOR", leftPos + 14, topPos + 14, DUTheme.TEXT_GREEN, false);

        // Fuel slot framed on the left; the slot itself lives at GUI (19, 31) in the menu.
        DUTheme.box(g, leftPos + 14, topPos + 26, 26, 26, 0xAA050805, DUTheme.PANEL_BORDER);
        g.drawString(font, "FUEL", leftPos + 16, topPos + 54, DUTheme.TEXT_DIM, false);

        // Status read-outs on the right, clear of the slot.
        g.drawString(font, menu.fePerTick() + " FE/t from fuel", leftPos + 48, topPos + 28, DUTheme.TEXT_DIM, false);

        int energyPct = menu.energyPercent();
        int burnPct = menu.burnPercent();
        g.drawString(font, "ENERGY", leftPos + 48, topPos + 40, DUTheme.TEXT_DIM, false);
        DUTheme.progress(g, leftPos + 88, topPos + 41, 52, 8, energyPct / 100f, DUTheme.PROGRESS_BLUE);
        g.drawString(font, energyPct + "%", leftPos + 144, topPos + 40, DUTheme.TEXT_DIM, false);

        g.drawString(font, "BURN", leftPos + 48, topPos + 52, DUTheme.TEXT_DIM, false);
        DUTheme.progress(g, leftPos + 88, topPos + 53, 52, 8, burnPct / 100f, burnPct > 0 ? DUTheme.PROGRESS_ORANGE : DUTheme.PANEL_ALT);
        g.drawString(font, burnPct > 0 ? burnPct + "%" : "IDLE", leftPos + 144, topPos + 52, burnPct > 0 ? DUTheme.WARN : DUTheme.TEXT_DIM, false);

        g.drawString(font, "Inventory", leftPos + 8, topPos + 72, DUTheme.TEXT_DIM, false);

        // Vanilla-style slot wells behind every slot (fuel + player inventory).
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            DUTheme.slotWell(g, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
    }
}
