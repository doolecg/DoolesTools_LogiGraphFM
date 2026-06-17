package net.doole.doolestools.logistics.switchboard;

import net.doole.doolestools.blockentity.NetworkSwitchboardBlockEntity;
import net.doole.doolestools.logistics.LinkType;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class SwitchboardNetworkAccess {
    private SwitchboardNetworkAccess() {}

    public record NetworkRef(String id, String name) {}

    public static List<NetworkRef> visibleNetworks(ServerLevel level, String originId, String originName) {
        Map<String, NetworkRef> visible = new LinkedHashMap<>();
        if (originId == null || originId.isBlank()) return List.of();
        visible.put(originId, new NetworkRef(originId, label(originId, originName)));
        Queue<String> queue = new ArrayDeque<>();
        queue.add(originId);
        Set<String> seen = new java.util.HashSet<>();
        seen.add(originId);
        while (!queue.isEmpty()) {
            String current = queue.remove();
            for (SwitchboardLinkData link : allLinks(level)) {
                if (!link.touches(current)) continue;
                String other = link.other(current);
                if (other.isBlank() || !seen.add(other)) continue;
                visible.put(other, new NetworkRef(other, label(other, NetworkSwitchboardBlockEntity.networkDisplayName(level, other))));
                queue.add(other);
            }
        }
        return List.copyOf(visible.values());
    }

    public static boolean canRoute(ServerLevel level, String sourceNetworkId, String targetNetworkId, LinkType type) {
        if (sourceNetworkId == null || targetNetworkId == null) return false;
        if (sourceNetworkId.isBlank() || targetNetworkId.isBlank() || sourceNetworkId.equals(targetNetworkId)) return true;
        for (SwitchboardLinkData link : allLinks(level)) {
            if (link.connects(sourceNetworkId, targetNetworkId) && link.allows(type)) return true;
        }
        return false;
    }

    public static int priority(ServerLevel level, String sourceNetworkId, String targetNetworkId, LinkType type) {
        if (sourceNetworkId == null || targetNetworkId == null || sourceNetworkId.equals(targetNetworkId)) return 0;
        int best = 0;
        for (SwitchboardLinkData link : allLinks(level)) {
            if (link.connects(sourceNetworkId, targetNetworkId) && link.allows(type)) best = Math.max(best, link.priority());
        }
        return best;
    }

    private static List<SwitchboardLinkData> allLinks(ServerLevel level) {
        List<SwitchboardLinkData> links = new ArrayList<>();
        NetworkSwitchboardBlockEntity.forEachLoaded(level, switchboard -> links.addAll(switchboard.links()));
        return links;
    }

    private static String label(String id, String name) {
        return name == null || name.isBlank() ? id : name;
    }
}
