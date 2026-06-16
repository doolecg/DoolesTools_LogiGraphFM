package net.doole.doolestools.client;

import net.doole.doolestools.client.screen.LogisticsComputerScreen;
import net.doole.doolestools.client.screen.LogisticsMonitorScreen;
import net.doole.doolestools.network.payload.ComputerStatePayload;
import net.doole.doolestools.network.payload.KnownNetworksPayload;
import net.doole.doolestools.network.payload.MonitorStatePayload;
import net.doole.doolestools.network.payload.NearbyLabelsPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-only S2C handlers. Loaded lazily by the registration lambdas in {@code ModNetworking}, so
 * these classes are never touched on a dedicated server.
 */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {}

    public static void handleComputerState(ComputerStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof LogisticsComputerScreen screen
                    && screen.context().pos().equals(payload.pos())) {
                screen.context().setState(payload.scan(), payload.graph(), payload.lastScanTime(), payload.power());
                screen.context().setActiveRouteIds(payload.activeRouteIds());
                screen.context().setNetworkState(payload.networkId(), payload.networkName(), payload.accessMode(), payload.editorWhitelist(), payload.canEdit());
                screen.context().setPowerHistory(payload.powerSupplyHistory(), payload.powerDemandHistory());
            }
        });
    }

    public static void handleNearbyLabels(NearbyLabelsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> LabelHologramStore.replaceAll(payload.positions(), payload.labels()));
    }

    public static void handleKnownNetworks(KnownNetworksPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientKnownNetworks.replaceAll(payload.ids(), payload.names(), payload.editable());
            if (Minecraft.getInstance().screen instanceof net.doole.doolestools.client.screen.NetworkEndpointNameScreen screen) {
                screen.onKnownNetworksUpdated();
            }
        });
    }

    public static void handleMonitorState(MonitorStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof LogisticsMonitorScreen screen
                    && screen.monitorPos().equals(payload.pos())) {
                screen.applyState(payload.linked(), payload.computerPos(), payload.mode(),
                        payload.graph(), payload.scan());
            }
        });
    }
}
