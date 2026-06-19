package net.doole.doolestools.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.doole.doolestools.blockentity.LogiGraphWallMonitorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class LogiGraphWallMonitorRenderer implements BlockEntityRenderer<LogiGraphWallMonitorBlockEntity> {
    public LogiGraphWallMonitorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LogiGraphWallMonitorBlockEntity monitor, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Chunk-safe placeholder renderer for 1.21.1; monitor state is still synced for client screens.
    }
}
