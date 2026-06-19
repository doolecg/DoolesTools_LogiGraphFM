package net.doole.doolestools.item;

import net.doole.doolestools.world.BlockLabelSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

/**
 * Multiplayer-safe label tool. The gun remembers a name (stored as the stack's custom name) until it
 * is changed: a normal right-click opens the naming screen, and a sneak-right-click stamps the
 * remembered name onto the clicked block. Labels are resolved into future scans and shown as
 * holograms while the gun is held.
 */
public class LabelGunItem extends Item {
    public LabelGunItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        String name = gunName(stack);

        if (player.isShiftKeyDown()) {
            BlockPos pos = context.getClickedPos();
            if (context.getLevel() instanceof ServerLevel level) {
                if (name.isBlank()) {
                    player.displayClientMessage(Component.literal("Set a name on the Label Gun first."), true);
                } else {
                    BlockLabelSavedData.get(level).setLabel(level.dimension().location(), pos, name);
                    player.displayClientMessage(Component.literal("Labelled: " + name), true);
                }
            } else if (!name.isBlank()) {
                applyLocalLabel(pos, name); // optimistic hologram update on the client
            }
            return InteractionResult.SUCCESS;
        }

        // Normal use: open the naming screen (client only).
        if (context.getLevel().isClientSide()) {
            openClientScreen(name);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (level.isClientSide()) {
            openClientScreen(gunName(stack));
        }
        return InteractionResultHolder.success(stack);
    }

    private static String gunName(ItemStack stack) {
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        return custom == null ? "" : custom.getString();
    }

    private static void openClientScreen(String currentName) {
        try {
            Class<?> bridge = Class.forName("net.doole.doolestools.client.LabelGunClientBridge");
            Method open = bridge.getMethod("open", String.class);
            open.invoke(null, currentName);
        } catch (ReflectiveOperationException ignored) {
            // Dedicated servers never execute this path; client failures should not break item use.
        }
    }

    private static void applyLocalLabel(BlockPos pos, String name) {
        try {
            Class<?> bridge = Class.forName("net.doole.doolestools.client.LabelGunClientBridge");
            Method apply = bridge.getMethod("applyLocal", BlockPos.class, String.class);
            apply.invoke(null, pos, name);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
