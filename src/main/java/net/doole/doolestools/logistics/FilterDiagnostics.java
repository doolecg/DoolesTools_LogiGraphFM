package net.doole.doolestools.logistics;

import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.LogisticsGraphData;

import java.util.ArrayList;
import java.util.List;

public record FilterDiagnostics(int inboundItemRoutes,
                                int outboundItemRoutes,
                                int ghostItems,
                                List<String> warnings) {
    public static FilterDiagnostics inspect(LogisticsGraphData graph, GraphNodeData node, FilterSettings settings) {
        if (graph == null || node == null || node.type() != NodeType.FILTER) {
            return new FilterDiagnostics(0, 0, 0, List.of());
        }
        int inbound = 0;
        int outbound = 0;
        for (GraphLinkData link : graph.activeCanvas().links()) {
            if (link.type() != LinkType.ITEMS) continue;
            if (node.nodeId().equals(link.targetNodeId())) inbound++;
            if (node.nodeId().equals(link.sourceNodeId())) outbound++;
        }
        int ghostItems = 0;
        for (String item : settings.paddedItems()) {
            if (item != null && !item.isBlank()) ghostItems++;
        }
        List<String> warnings = new ArrayList<>();
        if (inbound == 0) warnings.add("No inbound item route.");
        if (outbound == 0) warnings.add("No outbound item route.");
        if (settings.mode() == FilterSettings.Mode.WHITELIST && ghostItems == 0 && settings.legacyTokens().isEmpty()) {
            warnings.add("Whitelist has no item matches.");
        }
        return new FilterDiagnostics(inbound, outbound, ghostItems, List.copyOf(warnings));
    }
}
