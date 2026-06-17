package net.doole.doolestools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.doole.doolestools.DoolesTools;
import net.doole.doolestools.block.NetworkWireBlock;
import net.doole.doolestools.blockentity.NetworkWireBlockEntity;
import net.doole.doolestools.registry.ModBlocks;
import net.doole.doolestools.registry.ModItems;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = DoolesTools.MOD_ID, value = Dist.CLIENT)
public final class NetworkPlacementPreviewEvents {
    private NetworkPlacementPreviewEvents() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        if (!holdingEndpoint(player)) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        PlacementPreview preview = previewForHit(mc, hit);
        if (preview == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.position();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());
        ShapeRenderer.renderShape(pose, lines, endpointShape(preview.face()),
                preview.pos().getX() - cam.x, preview.pos().getY() - cam.y, preview.pos().getZ() - cam.z, 0xCC3FD2E0, 2.0f);
        buffers.endBatch(RenderTypes.lines());
    }

    private static PlacementPreview previewForHit(Minecraft mc, BlockHitResult hit) {
        BlockPos hitPos = hit.getBlockPos();
        if (mc.level.getBlockState(hitPos).is(ModBlocks.NETWORK_WIRE.get())) {
            if (!(mc.level.getBlockEntity(hitPos) instanceof NetworkWireBlockEntity wire) || wire.hasEndpoint()) return null;
            Direction face = NetworkWireBlock.endpointInstallFace(mc.level, hitPos, hit.getDirection());
            return face == null ? null : new PlacementPreview(hitPos, face);
        }

        BlockPos wirePos = hitPos.relative(hit.getDirection());
        if (!mc.level.getBlockState(wirePos).is(ModBlocks.NETWORK_WIRE.get())) return null;
        if (!(mc.level.getBlockEntity(wirePos) instanceof NetworkWireBlockEntity wire) || wire.hasEndpoint()) return null;
        return new PlacementPreview(wirePos, hit.getDirection().getOpposite());
    }

    private static boolean holdingEndpoint(LocalPlayer player) {
        return isEndpoint(player.getItemInHand(InteractionHand.MAIN_HAND)) || isEndpoint(player.getItemInHand(InteractionHand.OFF_HAND));
    }

    private static boolean isEndpoint(ItemStack stack) {
        return stack.getItem() == ModItems.WIRELESS_ROUTER.get()
                || stack.getItem() == ModItems.WIRELESS_DONGLE.get()
                || stack.getItem() == ModItems.NETWORK_MODEM.get();
    }

    private static VoxelShape endpointShape(Direction face) {
        return switch (face) {
            case DOWN -> Block.box(3, 0, 3, 13, 2, 13);
            case UP -> Block.box(3, 14, 3, 13, 16, 13);
            case NORTH -> Block.box(3, 3, 0, 13, 13, 2);
            case SOUTH -> Block.box(3, 3, 14, 13, 13, 16);
            case WEST -> Block.box(0, 3, 3, 2, 13, 13);
            case EAST -> Block.box(14, 3, 3, 16, 13, 13);
        };
    }

    private record PlacementPreview(BlockPos pos, Direction face) {}
}
