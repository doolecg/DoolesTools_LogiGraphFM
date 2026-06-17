package net.doole.doolestools.logistics;

import net.doole.doolestools.logistics.data.FurnaceSummary;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.InventorySummary;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.data.WarningData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure, side-free warning rules for the MVP. Operates only on already-collected snapshots, so it
 * is safe to call on either side. No world access, no mutation.
 */
public final class WarningGenerator {
    private WarningGenerator() {}

    public static final int NEARLY_FULL_PERCENT = 85;

    /** Per-block warnings derived during a scan. */
    public static List<WarningData> forScannedBlock(ScannedType type, InventorySummary inv, FurnaceSummary furnace) {
        List<WarningData> out = new ArrayList<>();
        // Furnaces report state through their own status below; their 3 slots (input/fuel/output) being
        // occupied is normal operation, not a "Full" storage condition, so skip the slot-fill rules.
        boolean isFurnace = furnace != null && furnace.hasData();
        if (!isFurnace && inv != null && inv.hasData()) {
            int pct = inv.fillPercent();
            boolean isTransportOrMachine = type == ScannedType.TRANSPORT
                    || type == ScannedType.MACHINE || type == ScannedType.UNKNOWN_MACHINE;
            boolean isStorage = type == ScannedType.STORAGE || type == ScannedType.UNKNOWN_STORAGE;
            if (inv.usedSlots() == 0) {
                out.add(WarningData.info("Empty"));
            } else if (inv.usedSlots() >= inv.totalSlots()) {
                // Transport and machine-type blocks full is advisory — expected during normal operation.
                // Storage full is actionable (WARNING); escalation to ERROR is done in forGraph().
                if (isTransportOrMachine) {
                    out.add(WarningData.info("Full"));
                } else {
                    out.add(WarningData.warning("Full"));
                }
            } else if (pct >= NEARLY_FULL_PERCENT) {
                if (isStorage) {
                    out.add(WarningData.warning("Nearly full (" + pct + "%)"));
                } else {
                    out.add(WarningData.info("Nearly full (" + pct + "%)"));
                }
            }
        }
        if (furnace != null && furnace.hasData()) {
            switch (furnace.status()) {
                case "No Fuel" -> out.add(WarningData.warning("No fuel"));
                case "Output Full" -> out.add(WarningData.error("Output full"));
                case "Not Progressing" -> out.add(WarningData.warning("Not progressing"));
                // "Standby" / "No Recipe" / "Idle" are benign states, not warnings.
                default -> { }
            }
        }
        return out;
    }

    /** Graph-structure warnings, computed from the saved graph and the latest scan index. */
    public static List<WarningData> forGraph(LogisticsGraphData graph, Map<String, ScannedBlockData> scanById) {
        List<WarningData> out = new ArrayList<>();
        if (graph == null || graph.isEmpty()) return out;

        Set<String> hasOutgoing = new HashSet<>();
        Set<String> hasIncoming = new HashSet<>();
        for (GraphLinkData link : graph.activeCanvas().links()) {
            hasOutgoing.add(link.sourceNodeId());
            hasIncoming.add(link.targetNodeId());
        }

        for (GraphNodeData node : graph.activeCanvas().nodes()) {
            boolean linked = hasOutgoing.contains(node.nodeId()) || hasIncoming.contains(node.nodeId());
            if (!linked) {
                out.add(WarningData.info(node.displayName() + ": not linked"));
            }
            switch (node.type()) {
                case SOURCE -> {
                    if (!hasOutgoing.contains(node.nodeId())) {
                        out.add(WarningData.warning(node.displayName() + ": source has no output"));
                    }
                }
                case SINK -> {
                    if (!hasIncoming.contains(node.nodeId())) {
                        out.add(WarningData.warning(node.displayName() + ": sink has no input"));
                    }
                }
                default -> { /* no structural rule */ }
            }

            // Target-storage checks: escalate end-of-flow full storage to ERROR.
            ScannedBlockData scanned = scanById.get(node.scannedBlockId());
            if (scanned != null && scanned.inventory().hasData() && hasIncoming.contains(node.nodeId())) {
                boolean isEndOfFlow = !hasOutgoing.contains(node.nodeId());
                boolean isFull = scanned.inventory().usedSlots() >= scanned.inventory().totalSlots();
                int pct = scanned.inventory().fillPercent();
                if (isFull && isEndOfFlow && scanned.isStorageLike()) {
                    out.add(WarningData.error(node.displayName() + ": end-of-flow storage full"));
                } else if (pct >= NEARLY_FULL_PERCENT) {
                    out.add(WarningData.warning(node.displayName() + " nearly full (" + pct + "%)"));
                }
            }
        }
        return out;
    }
}
