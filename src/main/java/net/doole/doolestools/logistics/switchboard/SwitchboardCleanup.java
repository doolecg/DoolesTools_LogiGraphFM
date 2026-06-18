package net.doole.doolestools.logistics.switchboard;

import java.util.ArrayList;
import java.util.List;

public final class SwitchboardCleanup {
    private SwitchboardCleanup() {
    }

    public static Result removeNetwork(String networkId, List<SwitchboardLinkData> links, List<SwitchboardNodePositionData> nodePositions) {
        String target = networkId == null ? "" : networkId.trim();
        if (target.isBlank()) {
            return new Result(List.copyOf(links == null ? List.of() : links), List.copyOf(nodePositions == null ? List.of() : nodePositions));
        }

        List<SwitchboardLinkData> keptLinks = new ArrayList<>();
        if (links != null) {
            for (SwitchboardLinkData link : links) {
                if (link == null || linkTouchesNetwork(link.sourceNetworkId(), link.targetNetworkId(), target)) continue;
                keptLinks.add(link);
            }
        }

        List<SwitchboardNodePositionData> keptPositions = new ArrayList<>();
        if (nodePositions != null) {
            for (SwitchboardNodePositionData position : nodePositions) {
                if (position == null || nodeMatchesNetwork(position.networkId(), target)) continue;
                keptPositions.add(position);
            }
        }

        return new Result(List.copyOf(keptLinks), List.copyOf(keptPositions));
    }

    public static boolean linkTouchesNetwork(String sourceNetworkId, String targetNetworkId, String removedNetworkId) {
        String removed = normalize(removedNetworkId);
        return !removed.isBlank() && (removed.equals(normalize(sourceNetworkId)) || removed.equals(normalize(targetNetworkId)));
    }

    public static boolean nodeMatchesNetwork(String nodeNetworkId, String removedNetworkId) {
        String removed = normalize(removedNetworkId);
        return !removed.isBlank() && removed.equals(normalize(nodeNetworkId));
    }

    private static String normalize(String networkId) {
        return networkId == null ? "" : networkId.trim();
    }

    public record Result(List<SwitchboardLinkData> links, List<SwitchboardNodePositionData> nodePositions) {
    }
}
