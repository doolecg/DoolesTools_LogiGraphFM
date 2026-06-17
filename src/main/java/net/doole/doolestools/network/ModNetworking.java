package net.doole.doolestools.network;

import net.doole.doolestools.client.ClientPayloadHandlers;
import net.doole.doolestools.network.payload.ClearScanPayload;
import net.doole.doolestools.network.payload.ComputerStatePayload;
import net.doole.doolestools.network.payload.LinkedComputersPayload;
import net.doole.doolestools.network.payload.LinkComputerPayload;
import net.doole.doolestools.network.payload.MonitorStatePayload;
import net.doole.doolestools.network.payload.RequestComputerSyncPayload;
import net.doole.doolestools.network.payload.RequestMonitorSyncPayload;
import net.doole.doolestools.network.payload.RequestSwitchboardStatePayload;
import net.doole.doolestools.network.payload.RequestKnownNetworksPayload;
import net.doole.doolestools.network.payload.SaveGraphPayload;
import net.doole.doolestools.network.payload.SaveSwitchboardPayload;
import net.doole.doolestools.network.payload.ScanAreaPayload;
import net.doole.doolestools.network.payload.SetMonitorModePayload;
import net.doole.doolestools.network.payload.SetComputerNetworkSettingsPayload;
import net.doole.doolestools.network.payload.SetBlockLabelPayload;
import net.doole.doolestools.network.payload.SetGunLabelPayload;
import net.doole.doolestools.network.payload.SetNetworkEndpointNamePayload;
import net.doole.doolestools.network.payload.RequestNearbyLabelsPayload;
import net.doole.doolestools.network.payload.NearbyLabelsPayload;
import net.doole.doolestools.network.payload.KnownNetworksPayload;
import net.doole.doolestools.network.payload.SwitchboardStatePayload;
import net.doole.doolestools.network.payload.UnlinkComputerPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers all LogiGraph network payloads. */
public final class ModNetworking {
    private ModNetworking() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Client -> Server
        registrar.playToServer(RequestComputerSyncPayload.TYPE, RequestComputerSyncPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleRequestComputerSync);
        registrar.playToServer(ScanAreaPayload.TYPE, ScanAreaPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleScanArea);
        registrar.playToServer(ClearScanPayload.TYPE, ClearScanPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleClearScan);
        registrar.playToServer(SaveGraphPayload.TYPE, SaveGraphPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleSaveGraph);
        registrar.playToServer(SetComputerNetworkSettingsPayload.TYPE, SetComputerNetworkSettingsPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleSetComputerNetworkSettings);
        registrar.playToServer(RequestMonitorSyncPayload.TYPE, RequestMonitorSyncPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleRequestMonitorSync);
        registrar.playToServer(SetMonitorModePayload.TYPE, SetMonitorModePayload.STREAM_CODEC,
                ServerPayloadHandlers::handleSetMonitorMode);
        registrar.playToServer(SetBlockLabelPayload.TYPE, SetBlockLabelPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleSetBlockLabel);
        registrar.playToServer(SetGunLabelPayload.TYPE, SetGunLabelPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleSetGunLabel);
        registrar.playToServer(SetNetworkEndpointNamePayload.TYPE, SetNetworkEndpointNamePayload.STREAM_CODEC,
                ServerPayloadHandlers::handleSetNetworkEndpointName);
        registrar.playToServer(RequestNearbyLabelsPayload.TYPE, RequestNearbyLabelsPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleRequestNearbyLabels);
        registrar.playToServer(RequestKnownNetworksPayload.TYPE, RequestKnownNetworksPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleRequestKnownNetworks);
        registrar.playToServer(LinkComputerPayload.TYPE, LinkComputerPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleLinkComputer);
        registrar.playToServer(UnlinkComputerPayload.TYPE, UnlinkComputerPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleUnlinkComputer);
        registrar.playToServer(RequestSwitchboardStatePayload.TYPE, RequestSwitchboardStatePayload.STREAM_CODEC,
                ServerPayloadHandlers::handleRequestSwitchboardState);
        registrar.playToServer(SaveSwitchboardPayload.TYPE, SaveSwitchboardPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleSaveSwitchboard);
        // Server -> Client. Handlers live in a client-only class; the lambda bodies only class-load
        // ClientPayloadHandlers when actually executed on the physical client.
        registrar.playToClient(ComputerStatePayload.TYPE, ComputerStatePayload.STREAM_CODEC,
                (payload, ctx) -> ClientPayloadHandlers.handleComputerState(payload, ctx));
        registrar.playToClient(MonitorStatePayload.TYPE, MonitorStatePayload.STREAM_CODEC,
                (payload, ctx) -> ClientPayloadHandlers.handleMonitorState(payload, ctx));
        registrar.playToClient(NearbyLabelsPayload.TYPE, NearbyLabelsPayload.STREAM_CODEC,
                (payload, ctx) -> ClientPayloadHandlers.handleNearbyLabels(payload, ctx));
        registrar.playToClient(KnownNetworksPayload.TYPE, KnownNetworksPayload.STREAM_CODEC,
                (payload, ctx) -> ClientPayloadHandlers.handleKnownNetworks(payload, ctx));
        registrar.playToClient(LinkedComputersPayload.TYPE, LinkedComputersPayload.STREAM_CODEC,
                (payload, ctx) -> ClientPayloadHandlers.handleLinkedComputers(payload, ctx));
        registrar.playToClient(SwitchboardStatePayload.TYPE, SwitchboardStatePayload.STREAM_CODEC,
                (payload, ctx) -> ClientPayloadHandlers.handleSwitchboardState(payload, ctx));
    }
}
