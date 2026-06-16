package net.doole.doolestools.client.screen;

import net.doole.doolestools.client.ClientNetworkSender;
import net.doole.doolestools.client.ClientPrefs;
import net.doole.doolestools.client.gui.DUTheme;
import net.doole.doolestools.client.gui.GuiSprites;
import net.doole.doolestools.client.gui.TerminalButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Client-only naming screen for the Label Gun. Sets the name the gun remembers; the player then
 * sneak-right-clicks blocks to stamp that name onto them. Recently used names are listed for re-use.
 */
public class LabelGunScreen extends Screen {
    private static final int PANEL_W = 220;
    private final String initialName;
    private EditBox labelBox;

    public LabelGunScreen(String initialName) {
        super(Component.literal("Label Gun"));
        this.initialName = initialName == null ? "" : initialName;
    }

    public static void open(String currentName) {
        Minecraft.getInstance().setScreen(new LabelGunScreen(currentName));
    }

    private int panelH() {
        return 92 + 14 + Math.min(ClientPrefs.MAX_RECENT_LABELS, ClientPrefs.recentLabels.size()) * 12;
    }

    @Override
    protected void init() {
        int x = (width - PANEL_W) / 2;
        int y = (height - panelH()) / 2;
        labelBox = new EditBox(font, x + 12, y + 34, PANEL_W - 24, 16, Component.literal("label"));
        labelBox.setMaxLength(48);
        labelBox.setValue(initialName);
        addRenderableWidget(labelBox);
        addRenderableWidget(new TerminalButton(x + 12, y + 60, 96, 16, Component.literal("Set Name"), this::saveAndClose)
                .sprite(GuiSprites.FILTER).accent(DUTheme.OK));
        // Clear only empties the input field — it no longer wipes the applied/remembered label.
        addRenderableWidget(new TerminalButton(x + 112, y + 60, 96, 16, Component.literal("Clear"), () -> {
            labelBox.setValue("");
            setFocused(labelBox);
            labelBox.setFocused(true);
        }).sprite(GuiSprites.CLEAR).accent(DUTheme.WARN));

        // Recent labels: click to load one back into the field.
        int ry = y + 92;
        int shown = Math.min(ClientPrefs.MAX_RECENT_LABELS, ClientPrefs.recentLabels.size());
        for (int i = 0; i < shown; i++) {
            String name = ClientPrefs.recentLabels.get(i);
            addRenderableWidget(new TerminalButton(x + 12, ry + i * 12, PANEL_W - 24, 11,
                    Component.literal(name), () -> { labelBox.setValue(name); setFocused(labelBox); labelBox.setFocused(true); })
                    .accent(DUTheme.PROGRESS_BLUE));
        }

        setFocused(labelBox);
        labelBox.setFocused(true);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int panelH = panelH();
        int x = (width - PANEL_W) / 2;
        int y = (height - panelH) / 2;
        DUTheme.bezel(g, x, y, PANEL_W, panelH);
        DUTheme.box(g, x + 6, y + 6, PANEL_W - 12, panelH - 12, DUTheme.SCREEN, DUTheme.PANEL_BORDER);
        g.text(font, "LABEL GUN", x + 12, y + 12, DUTheme.TEXT_GREEN, false);
        g.text(font, "Sneak + right-click to apply", x + 12, y + 22, DUTheme.TEXT_DIM, false);
        if (!ClientPrefs.recentLabels.isEmpty()) {
            g.text(font, "Recent (click to load):", x + 12, y + 82, DUTheme.TEXT_DIM, false);
        }
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
        String value = labelBox.getValue();
        ClientNetworkSender.setGunLabel(value);
        ClientPrefs.addRecentLabel(value);
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
