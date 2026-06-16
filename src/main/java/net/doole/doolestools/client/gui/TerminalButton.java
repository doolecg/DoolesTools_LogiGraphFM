package net.doole.doolestools.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** A chunky terminal-styled button used throughout the LogiGraph GUI. */
public class TerminalButton extends AbstractButton {

    private final Runnable onPress;
    private boolean toggled;
    private int accent = DUTheme.TEXT_GREEN;
    private Glyphs.Drawer icon;
    private Identifier sprite;
    private boolean iconOnly;

    public TerminalButton(int x, int y, int w, int h, Component label, Runnable onPress) {
        super(x, y, w, h, label);
        this.onPress = onPress;
    }

    public TerminalButton accent(int color) {
        this.accent = color;
        return this;
    }

    /** Adds a glyph to the left of the label. */
    public TerminalButton icon(Glyphs.Drawer icon) {
        this.icon = icon;
        this.sprite = null;
        return this;
    }

    /** Adds an image sprite to the left of the label. */
    public TerminalButton sprite(Identifier sprite) {
        this.sprite = sprite;
        this.icon = null;
        return this;
    }

    /** Renders only the glyph, centred (no text). */
    public TerminalButton iconOnly(Glyphs.Drawer icon) {
        this.icon = icon;
        this.sprite = null;
        this.iconOnly = true;
        return this;
    }

    public TerminalButton spriteOnly(Identifier sprite) {
        this.sprite = sprite;
        this.icon = null;
        this.iconOnly = true;
        return this;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public boolean isToggled() {
        return toggled;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (onPress != null) onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        boolean hover = isHoveredOrFocused();
        int fill = !active ? 0xFF161a14 : toggled ? 0xFF1c3320 : hover ? 0xFF1b2418 : DUTheme.PANEL;
        int border = !active ? DUTheme.PANEL_BORDER : (toggled || hover) ? accent : DUTheme.PANEL_BORDER;
        DUTheme.box(g, getX(), getY(), width, height, fill, border);
        int textColor = !active ? DUTheme.DISABLED : toggled ? accent : DUTheme.TEXT;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int iconColor = !active ? DUTheme.DISABLED : (toggled || hover) ? accent : DUTheme.TEXT_DIM;

        if (iconOnly && (icon != null || sprite != null)) {
            if (sprite != null) GuiSprites.draw(g, sprite, getX() + (width - 12) / 2, getY() + (height - 12) / 2, 12);
            else icon.draw(g, getX() + (width - 9) / 2, getY() + (height - 9) / 2, iconColor);
            return;
        }
        int ty = getY() + (height - 8) / 2;
        if (icon != null || sprite != null) {
            if (sprite != null) GuiSprites.draw(g, sprite, getX() + 3, getY() + (height - 12) / 2, 12);
            else icon.draw(g, getX() + 4, getY() + (height - 9) / 2, iconColor);
            int textX = getX() + 16;
            g.centeredText(font, getMessage(), (textX + getX() + width) / 2, ty, textColor);
        } else {
            g.centeredText(font, getMessage(), getX() + width / 2, ty, textColor);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
