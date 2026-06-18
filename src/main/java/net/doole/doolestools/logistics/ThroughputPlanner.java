package net.doole.doolestools.logistics;

import net.doole.doolestools.config.ModServerConfig;
import net.doole.doolestools.logistics.data.GraphLinkData;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.LogisticsGraphData;
import net.doole.doolestools.logistics.data.MachineProgressData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.easyfactory.EasyFactoryManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure static analysis over the saved graph + scan snapshot. No world access.
 * Estimates throughput capacity per link and flags bottlenecks.
 *
 * A "bottleneck" here just means the route cant keep up with the machine producing into it.
 * This is a rough estimate - real throughput depends on a lot of factors we cant know statically.
 */
public final class ThroughputPlanner {
    private ThroughputPlanner() {}

    public record LinkAnalysis(
        GraphLinkData link,
        String sourceName,
        String targetName,
        int capacityPerMin,   // items/mB/FE per minute this route can theoretically push
        int estimatedSourceRatePerMin, // estimated production rate at the source (0 if unknown)
        boolean isBottleneck,  // capacity < estimated source rate
        boolean isStarved      // source doesnt seem to produce anything (empty, no progress)
    ) {}

    public record PlannerResult(
        List<LinkAnalysis> links,
        int totalBottlenecks,
        int totalStarved
    ) {}

    public static PlannerResult analyse(LogisticsGraphData graph, List<ScannedBlockData> scan) {
        if (graph == null || graph.isEmpty()) return new PlannerResult(List.of(), 0, 0);
        Map<String, ScannedBlockData> byId = new HashMap<>();
        for (ScannedBlockData s : scan) byId.put(s.id(), s);
        Map<String, String> nodeNames = new HashMap<>();
        for (GraphNodeData n : graph.activeCanvas().nodes()) nodeNames.put(n.nodeId(), n.displayName());

        int tickInterval = Math.max(1, ModServerConfig.LFM_TICK_INTERVAL.get());
        int baseItemsPerRoute = ModServerConfig.MAX_ITEMS_MOVED_PER_ROUTE.get();
        int baseMbPerRoute = ModServerConfig.MAX_FLUID_MOVED_PER_ROUTE.get();
        int baseFePerRoute = ModServerConfig.MAX_ENERGY_MOVED_PER_ROUTE.get();
        // 20 ticks per second, 60 seconds per minute
        float ticksPerMin = 20f * 60f;
        float routesPerMin = ticksPerMin / tickInterval;

        List<LinkAnalysis> results = new ArrayList<>();
        int bottlenecks = 0;
        int starved = 0;

        for (GraphLinkData link : graph.activeCanvas().links()) {
            if (link.type() == LinkType.MANUAL) continue;
            GraphNodeData sourceNode = graph.findNode(link.sourceNodeId());
            GraphNodeData targetNode = graph.findNode(link.targetNodeId());
            if (sourceNode == null || targetNode == null) continue;

            // skip routing nodes - they dont have real capacity on their own
            if (EasyFactoryManager.isRoutingNode(sourceNode.type()) || EasyFactoryManager.isRoutingNode(targetNode.type())) continue;

            String sourceName = sourceNode.displayName();
            String targetName = targetNode.displayName();

            int capacityPerMin = switch (link.type()) {
                case ITEMS -> Math.round(baseItemsPerRoute * routesPerMin);
                case FLUIDS -> Math.round(baseMbPerRoute * routesPerMin);
                case ENERGY -> Math.round(baseFePerRoute * routesPerMin);
                default -> 0;
            };

            ScannedBlockData scanned = byId.get(sourceNode.scannedBlockId());
            int sourceRate = estimateSourceRate(scanned, link.type());
            boolean isStarved = scanned == null
                || (link.type() == LinkType.ITEMS && scanned.inventory().hasData() && scanned.inventory().usedSlots() == 0);
            boolean isBottleneck = sourceRate > 0 && capacityPerMin < sourceRate;

            if (isBottleneck) bottlenecks++;
            if (isStarved) starved++;

            results.add(new LinkAnalysis(link, sourceName, targetName, capacityPerMin, sourceRate, isBottleneck, isStarved));
        }
        return new PlannerResult(results, bottlenecks, starved);
    }

    private static int estimateSourceRate(ScannedBlockData scanned, LinkType type) {
        if (scanned == null) return 0;
        // rough estimate from progress data - we only have remainingTicks and percent
        // so we back-calculate total cycle length from those two values
        MachineProgressData progress = scanned.progress();
        if (progress != null && progress.present() && progress.active()) {
            if (type == LinkType.ITEMS) {
                float pct = progress.percent();
                long remaining = progress.remainingTicks();
                // need at least a little progress to estimate cycle length, avoid div by zero
                if (pct > 0.01f && pct < 1.0f && remaining > 0) {
                    float totalTicks = remaining / (1.0f - pct);
                    float recipesPerMin = (20f * 60f) / totalTicks;
                    return Math.round(recipesPerMin);
                }
            }
        }
        // for energy we can read the stored energy and guess transfer rate from fill level
        if (type == LinkType.ENERGY && scanned.energy().hasData()) {
            long capacity = scanned.energy().capacity();
            long stored = scanned.energy().stored();
            if (capacity > 0 && stored > (long) (capacity * 0.8)) {
                // looks like its nearly full, probably generating faster than consuming
                long rate = Math.min((long) Integer.MAX_VALUE / 2L, stored / 10L);
                return (int) rate;
            }
        }
        return 0;
    }
}
