package net.doole.doolestools.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.doole.doolestools.blockentity.LogiGraphWallMonitorBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class LogiGraphWallMonitorRenderer implements BlockEntityRenderer<LogiGraphWallMonitorBlockEntity, LogiGraphWallMonitorRenderer.State> {
    public LogiGraphWallMonitorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LogiGraphWallMonitorBlockEntity monitor, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(monitor, state, overlay);
        state.valid = monitor.valid();
        state.controller = monitor.controller();
        state.width = monitor.width();
        state.height = monitor.height();
        state.tileX = monitor.tileX();
        state.tileY = monitor.tileY();
        state.linked = !monitor.linkedComputer().equals(BlockPos.ZERO);
        state.mode = monitor.mode();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Render-state extraction is wired here; richer text/graph submission can be layered onto
        // the 26.1 submit pipeline without touching server logic or monitor validation.
    }

    public static class State extends BlockEntityRenderState {
        public boolean valid;
        public boolean linked;
        public BlockPos controller = BlockPos.ZERO;
        public int width = 1;
        public int height = 1;
        public int tileX;
        public int tileY;
        public int mode;
    }
}
