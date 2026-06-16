package net.doole.doolestools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.client.gui.DUTheme;
import net.doole.doolestools.item.LabelGunItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Map;

/**
 * Client-only behaviour for the Label Gun: periodically pulls nearby labels from the server while the
 * gun is held, and renders them as green see-through holograms over the labelled blocks.
 */
@EventBusSubscriber(modid = DoolesTools.MOD_ID, value = Dist.CLIENT)
public final class LabelGunClientEvents {
    private LabelGunClientEvents() {}

    private static final int REQUEST_INTERVAL = 20; // ticks
    private static final double MAX_RENDER_DIST_SQR = 48 * 48;
    private static int tickCounter;
    private static boolean wasHoldingGun;

    public static boolean holdingGun(LocalPlayer player) {
        if (player == null) return false;
        return player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof LabelGunItem
                || player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof LabelGunItem;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        boolean holding = holdingGun(mc.player);
        if (!holding) {
            if (wasHoldingGun) LabelHologramStore.clear();
            wasHoldingGun = false;
            tickCounter = 0;
            return;
        }
        if (!wasHoldingGun) {
            wasHoldingGun = true;
            tickCounter = 0;
            ClientNetworkSender.requestNearbyLabels();
            return;
        }
        if (++tickCounter >= REQUEST_INTERVAL) {
            tickCounter = 0;
            ClientNetworkSender.requestNearbyLabels();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !holdingGun(mc.player)) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.position();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        // Highlight the block the player is aiming at, so it's clear what the gun will label.
        boolean renderedText = false;
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos tp = hit.getBlockPos();
            VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());
            ShapeRenderer.renderShape(pose, lines, Shapes.block(),
                    tp.getX() - cam.x, tp.getY() - cam.y, tp.getZ() - cam.z, 0xCC40FF73, 2.0f);
            buffers.endBatch(RenderTypes.lines());
            String heldName = heldGunName(mc.player);
            if (!heldName.isBlank()) {
                renderLabel(pose, buffers, font, camera, cam, tp, heldName, bgColor(mc));
                renderedText = true;
            }
        }

        Map<BlockPos, String> labels = LabelHologramStore.snapshot();
        if (labels.isEmpty()) {
            if (renderedText) buffers.endBatch();
            return;
        }

        int bgColor = bgColor(mc);

        for (Map.Entry<BlockPos, String> e : labels.entrySet()) {
            BlockPos pos = e.getKey();
            String text = e.getValue();
            if (text == null || text.isBlank()) continue;
            renderLabel(pose, buffers, font, camera, cam, pos, text, bgColor);
            renderedText = true;
        }
        if (renderedText) buffers.endBatch();
    }

    private static int bgColor(Minecraft mc) {
        return (int) (mc.options.getBackgroundOpacity(0.25f) * 255f) << 24;
    }

    private static String heldGunName(LocalPlayer player) {
        String main = gunName(player.getItemInHand(InteractionHand.MAIN_HAND));
        if (!main.isBlank()) return main;
        return gunName(player.getItemInHand(InteractionHand.OFF_HAND));
    }

    private static String gunName(ItemStack stack) {
        if (!(stack.getItem() instanceof LabelGunItem)) return "";
        var custom = stack.get(DataComponents.CUSTOM_NAME);
        return custom == null ? "" : custom.getString();
    }

    private static void renderLabel(PoseStack pose, MultiBufferSource.BufferSource buffers, Font font,
                                    Camera camera, Vec3 cam, BlockPos pos, String text, int bgColor) {
        double cx = pos.getX() + 0.5;
        double cyTop = pos.getY() + 1.4;
        double cz = pos.getZ() + 0.5;
        if (cam.distanceToSqr(cx, cyTop, cz) > MAX_RENDER_DIST_SQR) return;
        pose.pushPose();
        pose.translate((float) (cx - cam.x), (float) (cyTop - cam.y), (float) (cz - cam.z));
        pose.mulPose(camera.rotation());
        pose.scale(-0.025f, -0.025f, 0.025f);
        Matrix4f matrix = pose.last().pose();
        float tx = -font.width(text) / 2f;
        font.drawInBatch(text, tx, 0f, DUTheme.OK, false, matrix, buffers,
                Font.DisplayMode.SEE_THROUGH, bgColor, 0xF000F0);
        pose.popPose();
    }
}
