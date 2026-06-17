package net.doole.doolestools.client.screen;

import net.doole.doolestools.client.ClientNetworkSender;
import net.doole.doolestools.client.ClientKnownNetworks;
import net.doole.doolestools.client.gui.DUTheme;
import net.doole.doolestools.client.gui.GuiSprites;
import net.doole.doolestools.client.gui.TerminalButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class NetworkEndpointNameScreen extends Screen {
    private static final int PANEL_W = 260;
    private static final int PANEL_H = 214;
    private final BlockPos pos;
    private final String title;
    private final String initialName;
    private final String initialId;
    private final int[] upgradeCounts;
    @Nullable private final Direction endpointFace;
    private int networkIndex;
    private boolean networkDropdownOpen;
    private EditBox idBox;
    private EditBox nicknameBox;
    private TerminalButton networkButton;

    public NetworkEndpointNameScreen(BlockPos pos, String title, String initialName, String initialId, int[] upgradeCounts, @Nullable Direction endpointFace) {
        super(Component.literal(title));
        this.pos = pos;
        this.title = title == null || title.isBlank() ? "Network Endpoint" : title;
        this.initialName = initialName == null ? "" : initialName;
        this.initialId = initialId == null || initialId.isBlank() ? "0000" : initialId;
        this.upgradeCounts = normalizeUpgradeCounts(upgradeCounts);
        this.endpointFace = endpointFace;
    }

    public static void open(BlockPos pos, String title, String currentName, String currentId, int[] upgradeCounts, @Nullable Direction face) {
        Minecraft.getInstance().setScreen(new NetworkEndpointNameScreen(pos, title, currentName, currentId, upgradeCounts, face));
    }

    @Override
    protected void init() {
        int x = (width - PANEL_W) / 2;
        int y = (height - PANEL_H) / 2;
        idBox = new EditBox(font, x + 78, y + 34, PANEL_W - 90, 14, Component.literal("id"));
        idBox.setMaxLength(48);
        idBox.setValue(initialId);
        idBox.setEditable(false);
        addRenderableWidget(idBox);
        networkButton = new TerminalButton(x + 78, y + 52, PANEL_W - 90, 14, Component.literal(networkLabel()), this::toggleNetworkDropdown)
                .accent(DUTheme.PROGRESS_BLUE);
        addRenderableWidget(networkButton);
        nicknameBox = new EditBox(font, x + 78, y + 70, PANEL_W - 90, 14, Component.literal("nickname"));
        nicknameBox.setMaxLength(48);
        nicknameBox.setValue(initialName);
        addRenderableWidget(nicknameBox);
        addRenderableWidget(new TerminalButton(x + 12, y + 190, 116, 16, Component.literal("Set Fields"), this::saveAndClose)
                .sprite(GuiSprites.FILTER).accent(DUTheme.OK));
        addRenderableWidget(new TerminalButton(x + 132, y + 190, 116, 16, Component.literal("Clear"), () -> {
            nicknameBox.setValue("");
            setFocused(nicknameBox);
            nicknameBox.setFocused(true);
            nicknameBox.moveCursorToEnd(false);
        }).sprite(GuiSprites.CLEAR).accent(DUTheme.WARN));
        setFocused(nicknameBox);
        nicknameBox.setFocused(true);
        ClientNetworkSender.requestKnownNetworks();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = (width - PANEL_W) / 2;
        int y = (height - PANEL_H) / 2;
        DUTheme.bezel(g, x, y, PANEL_W, PANEL_H);
        DUTheme.box(g, x + 6, y + 6, PANEL_W - 12, PANEL_H - 12, DUTheme.SCREEN, DUTheme.PANEL_BORDER);
        g.text(font, title.toUpperCase(java.util.Locale.ROOT), x + 12, y + 12, DUTheme.TEXT_GREEN, false);
        String subtitle = endpointFace != null ? "Endpoint identity (" + endpointFace.getSerializedName() + " face)" : "Endpoint identity";
        g.text(font, subtitle, x + 12, y + 22, DUTheme.TEXT_DIM, false);
        g.text(font, "ID", x + 12, y + 37, DUTheme.TEXT_DIM, false);
        g.text(font, "NETWORK", x + 12, y + 55, DUTheme.TEXT_DIM, false);
        g.text(font, "NICKNAME", x + 12, y + 73, DUTheme.TEXT_DIM, false);
        g.text(font, statusLabel(), x + 12, y + 90, statusColor(), false);
        renderUpgradeRows(g, x + 12, y + 106);
        // Tell the player how cards get in and out — the install path is an in-world interaction,
        // not a button on this screen, so it isn't obvious otherwise.
        g.text(font, "Right-click the device with a card to install", x + 12, y + 162, DUTheme.TEXT_DIM, false);
        g.text(font, "Right-click with the screwdriver to remove one", x + 12, y + 172, DUTheme.TEXT_DIM, false);
        renderNetworkDropdown(g, x + 78, y + 67, PANEL_W - 90);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (networkDropdownOpen) {
            int x = (width - PANEL_W) / 2 + 78;
            int y = (height - PANEL_H) / 2 + 67;
            int row = networkDropdownRowAt(event.x(), event.y(), x, y, PANEL_W - 90);
            if (row >= 0) {
                networkIndex = row;
                networkDropdownOpen = false;
                refreshNetworkButton();
                return true;
            }
            if (!(networkButton != null && networkButton.isMouseOver(event.x(), event.y()))) networkDropdownOpen = false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 257 || event.key() == 335) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void saveAndClose() {
        if (endpointFace != null) {
            ClientNetworkSender.setNetworkEndpointIdentity(pos, nicknameBox.getValue(), selectedNetworkId(), endpointFace);
        } else {
            ClientNetworkSender.setNetworkEndpointIdentity(pos, nicknameBox.getValue(), selectedNetworkId());
        }
        onClose();
    }

    private void toggleNetworkDropdown() {
        var entries = ClientKnownNetworks.entries();
        if (entries.isEmpty()) return;
        networkDropdownOpen = !networkDropdownOpen;
    }

    private void renderNetworkDropdown(GuiGraphicsExtractor g, int x, int y, int w) {
        if (!networkDropdownOpen) return;
        var entries = ClientKnownNetworks.entries();
        if (entries.isEmpty()) return;
        int visible = Math.min(6, entries.size());
        DUTheme.box(g, x, y, w, visible * 15 + 2, DUTheme.SCREEN, DUTheme.SELECTED);
        for (int i = 0; i < visible; i++) {
            var entry = entries.get(i);
            int rowY = y + 1 + i * 15;
            boolean selected = i == Math.floorMod(networkIndex, entries.size());
            g.fill(x + 1, rowY, x + w - 1, rowY + 14, selected ? 0xFF14303a : DUTheme.PANEL);
            String text = entry.name() + "  " + (entry.editable() ? "EDIT" : "VIEW");
            g.text(font, trim(text, w - 8), x + 4, rowY + 3, selected ? DUTheme.SELECTED : DUTheme.TEXT, false);
        }
    }

    private void renderUpgradeRows(GuiGraphicsExtractor g, int x, int y) {
        g.text(font, "INSTALLED UPGRADES", x, y, DUTheme.TEXT_GREEN, false);
        String[] labels = { "Speed", "Stack", "Range", "Efficiency" };
        for (int i = 0; i < labels.length; i++) {
            int value = upgradeCounts[i];
            int rowY = y + 13 + i * 10;
            int color = value < 0 ? DUTheme.TEXT_DIM : value > 0 ? DUTheme.OK : DUTheme.WARN;
            String count = value < 0 ? "n/a" : value + " / 4";
            g.text(font, labels[i], x, rowY, DUTheme.TEXT_DIM, false);
            g.text(font, count, x + 96, rowY, color, false);
        }
    }

    private int networkDropdownRowAt(double mx, double my, int x, int y, int w) {
        var entries = ClientKnownNetworks.entries();
        if (entries.isEmpty()) return -1;
        int visible = Math.min(6, entries.size());
        if (mx < x || mx > x + w || my < y || my > y + visible * 15 + 2) return -1;
        int row = (int) ((my - y - 1) / 15);
        return row >= 0 && row < visible ? row : -1;
    }

    private String selectedNetworkId() {
        var entries = ClientKnownNetworks.entries();
        return entries.isEmpty() ? "" : entries.get(Math.floorMod(networkIndex, entries.size())).id();
    }

    private String networkLabel() {
        var entries = ClientKnownNetworks.entries();
        if (entries.isEmpty()) return "Unassigned";
        var entry = entries.get(Math.floorMod(networkIndex, entries.size()));
        return entry.name() + " (" + (entry.editable() ? "EDIT" : "VIEW") + ")";
    }

    public void onKnownNetworksUpdated() {
        var entries = ClientKnownNetworks.entries();
        if (!entries.isEmpty()) networkIndex = Math.floorMod(networkIndex, entries.size());
        refreshNetworkButton();
    }

    private String trim(String value, int width) {
        if (font.width(value) <= width) return value;
        String ellipsis = "...";
        int max = Math.max(0, width - font.width(ellipsis));
        String out = value;
        while (!out.isEmpty() && font.width(out) > max) out = out.substring(0, out.length() - 1);
        return out + ellipsis;
    }

    private void refreshNetworkButton() {
        if (networkButton != null) networkButton.setMessage(Component.literal(networkLabel()));
    }

    private String statusLabel() {
        var entries = ClientKnownNetworks.entries();
        if (entries.isEmpty()) return "No editable networks found";
        var entry = entries.get(Math.floorMod(networkIndex, entries.size()));
        return entry.editable() ? "Assignable network" : "View-only network";
    }

    private int statusColor() {
        var entries = ClientKnownNetworks.entries();
        if (entries.isEmpty()) return DUTheme.WARN;
        return entries.get(Math.floorMod(networkIndex, entries.size())).editable() ? DUTheme.OK : DUTheme.TEXT_DIM;
    }

    private static int[] normalizeUpgradeCounts(int[] values) {
        int[] out = new int[] { 0, 0, 0, 0 };
        if (values == null) return out;
        for (int i = 0; i < out.length && i < values.length; i++) out[i] = values[i];
        return out;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
