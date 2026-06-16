package net.doole.doolestools.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only resolver/renderer for real item icons from registry ids. Resolved stacks are cached.
 * Call sites fall back to a flat coloured square when no icon is available (air / unknown / modded
 * blocks without an item form).
 */
public final class ItemIcons {
    private ItemIcons() {}

    /** Native vanilla item icon size. */
    public static final int SIZE = 16;

    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    public static ItemStack stackFor(String registryId) {
        if (registryId == null || registryId.isEmpty()) return ItemStack.EMPTY;
        return CACHE.computeIfAbsent(registryId, ItemIcons::resolve);
    }

    private static ItemStack resolve(String id) {
        Identifier rl = Identifier.tryParse(id);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(rl);
        if (item != null && item != Items.AIR) return new ItemStack(item);
        Block block = BuiltInRegistries.BLOCK.getValue(rl);
        if (block != null) {
            ItemStack stack = new ItemStack(block);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Draws the item icon for {@code registryId} at ({@code x},{@code y}). If no icon resolves, draws a
     * flat {@code fallbackColor} square ({@code size}px) and returns {@code false}.
     */
    public static boolean render(GuiGraphicsExtractor g, String registryId, int x, int y, int size, int fallbackColor) {
        ItemStack stack = net.doole.doolestools.client.ClientPrefs.showItemIcons ? stackFor(registryId) : ItemStack.EMPTY;
        if (stack.isEmpty()) {
            g.fill(x, y, x + size, y + size, fallbackColor);
            DUTheme.outline(g, x, y, size, size, 0xFF000000);
            return false;
        }
        g.item(stack, x, y);
        return true;
    }
}
