package net.doole.doolestools.logistics;

import net.doole.doolestools.logistics.data.GraphCanvasData;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.LogisticsGraphData;

import java.util.HashSet;
import java.util.Set;

public final class GraphImportValidator {
    public static final int MAX_NODES = 512;
    public static final int MAX_LINKS = 2048;
    public static final int MAX_COORDINATE = 1_000_000;

    private GraphImportValidator() {}

    public static ValidationResult validate(LogisticsGraphData graph) {
        GraphCanvasData canvas = graph.activeCanvas();
        if (canvas.nodes().size() > MAX_NODES) return ValidationResult.error("Too many nodes");
        if (canvas.links().size() > MAX_LINKS) return ValidationResult.error("Too many links");

        Set<String> nodeIds = new HashSet<>();
        for (GraphNodeData node : canvas.nodes()) {
            if (node.nodeId().isBlank()) return ValidationResult.error("Blank node id");
            if (!nodeIds.add(node.nodeId())) return ValidationResult.error("Duplicate node id: " + node.nodeId());
            if (Math.abs(node.x()) > MAX_COORDINATE || Math.abs(node.y()) > MAX_COORDINATE) {
                return ValidationResult.error("Node coordinate out of bounds: " + node.nodeId());
            }
        }

        Set<String> linkIds = new HashSet<>();
        for (GraphLinkData link : canvas.links()) {
            if (link.linkId().isBlank()) return ValidationResult.error("Blank link id");
            if (!linkIds.add(link.linkId())) return ValidationResult.error("Duplicate link id: " + link.linkId());
            if (!nodeIds.contains(link.sourceNodeId()) || !nodeIds.contains(link.targetNodeId())) {
                return ValidationResult.error("Link references missing node: " + link.linkId());
            }
            if (link.sourcePortId().isBlank() || link.targetPortId().isBlank()) {
                return ValidationResult.error("Link references blank port: " + link.linkId());
            }
        }
        return ValidationResult.OK;
    }

    public record ValidationResult(boolean ok, String message) {
        public static final ValidationResult OK = new ValidationResult(true, "OK");

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
}
