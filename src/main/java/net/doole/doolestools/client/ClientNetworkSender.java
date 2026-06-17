package net.doole.doolestools.client;

import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.switchboard.SwitchboardLinkData;
import net.doole.doolestools.logistics.switchboard.SwitchboardNodePositionData;
import net.doole.doolestools.network.payload.ClearScanPayload;
import net.doole.doolestools.network.payload.RequestComputerSyncPayload;
import net.doole.doolestools.network.payload.RequestMonitorSyncPayload;
import net.doole.doolestools.network.payload.RequestSwitchboardStatePayload;
import net.doole.doolestools.network.payload.SaveGraphPayload;
import net.doole.doolestools.network.payload.SaveSwitchboardPayload;
import net.doole.doolestools.network.payload.ScanAreaPayload;
import net.doole.doolestools.network.payload.SetMonitorModePayload;
import net.doole.doolestools.network.payload.SetComputerNetworkSettingsPayload;
import net.doole.doolestools.network.payload.SetBlockLabelPayload;
import net.doole.doolestools.network.payload.SetGunLabelPayload;
import net.doole.doolestools.network.payload.SetNetworkEndpointNamePayload;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

/** Thin client-only wrapper around {@link ClientPacketDistributor} for LogiGraph C2S payloads. */
public final class ClientNetworkSender {
    private ClientNetworkSender() {}

    public static void requestComputerSync(BlockPos pos) {
        ClientPacketDistributor.sendToServer(new RequestComputerSyncPayload(pos));
    }

    public static void scanArea(BlockPos pos) {
        ClientPacketDistributor.sendToServer(new ScanAreaPayload(pos));
    }

    public static void clearScan(BlockPos pos) {
        ClientPacketDistributor.sendToServer(new ClearScanPayload(pos));
    }

    public static void saveGraph(BlockPos pos, LogisticsGraphData graph) {
        ClientPacketDistributor.sendToServer(new SaveGraphPayload(pos, graph));
    }

    public static void setComputerNetworkSettings(BlockPos pos, String name, String accessMode, List<String> editorWhitelist) {
        ClientPacketDistributor.sendToServer(new SetComputerNetworkSettingsPayload(pos, name, accessMode, editorWhitelist));
    }

    public static void requestMonitorSync(BlockPos pos) {
        ClientPacketDistributor.sendToServer(new RequestMonitorSyncPayload(pos));
    }

    public static void setMonitorMode(BlockPos pos, int mode) {
        ClientPacketDistributor.sendToServer(new SetMonitorModePayload(pos, mode));
    }

    public static void setBlockLabel(BlockPos pos, String label) {
        ClientPacketDistributor.sendToServer(new SetBlockLabelPayload(pos, label));
    }

    public static void setGunLabel(String label) {
        ClientPacketDistributor.sendToServer(new SetGunLabelPayload(label));
    }

    public static void setNetworkEndpointName(BlockPos pos, String name) {
        ClientPacketDistributor.sendToServer(new SetNetworkEndpointNamePayload(pos, name, "", null));
    }

    public static void setNetworkEndpointIdentity(BlockPos pos, String name, String networkId) {
        ClientPacketDistributor.sendToServer(new SetNetworkEndpointNamePayload(pos, name, networkId, null));
    }

    public static void setNetworkEndpointIdentity(BlockPos pos, String name, String networkId, net.minecraft.core.Direction face) {
        ClientPacketDistributor.sendToServer(new SetNetworkEndpointNamePayload(pos, name, networkId, face));
    }

    public static void requestNearbyLabels() {
        ClientPacketDistributor.sendToServer(net.doole.doolestools.network.payload.RequestNearbyLabelsPayload.INSTANCE);
    }

    public static void requestKnownNetworks() {
        ClientPacketDistributor.sendToServer(net.doole.doolestools.network.payload.RequestKnownNetworksPayload.INSTANCE);
    }

    public static void requestSwitchboardState(BlockPos pos) {
        ClientPacketDistributor.sendToServer(new RequestSwitchboardStatePayload(pos));
    }

    public static void saveSwitchboard(BlockPos pos, List<SwitchboardLinkData> links, List<SwitchboardNodePositionData> nodePositions) {
        ClientPacketDistributor.sendToServer(new SaveSwitchboardPayload(pos, links, nodePositions));
    }

}
