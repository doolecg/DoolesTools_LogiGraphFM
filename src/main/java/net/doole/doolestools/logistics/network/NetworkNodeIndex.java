package net.doole.doolestools.logistics.network;

import net.doole.doolestools.blockentity.NetworkEndpointBlockEntity;
import net.doole.doolestools.blockentity.NetworkRelayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Per-dimension registry of the network nodes that are otherwise expensive to find: standalone
 * relays and standalone wireless routers. AE2/RS/Powah all keep their grid nodes in a persistent
 * structure instead of re-scanning the world; this is the same idea, scoped to just the two block
 * entity types the power calculator used to hunt for by iterating every block entity in a chunk
 * radius.
 *
 * <p>Nodes register themselves in {@code onLoad()} and drop out in {@code setRemoved()}, so the
 * index only ever holds currently-loaded entities — matching the old "loaded chunks only" semantics
 * without the per-tick chunk walk. Iteration still guards against stale references defensively, so a
 * missed unload callback can never feed a removed block entity into the power maths.
 */
public final class NetworkNodeIndex {
    private static final Map<ResourceKey<Level>, NetworkNodeIndex> INDEXES = new ConcurrentHashMap<>();

    private final Map<BlockPos, NetworkRelayBlockEntity> relays = new ConcurrentHashMap<>();
    private final Map<BlockPos, NetworkEndpointBlockEntity> routers = new ConcurrentHashMap<>();

    private NetworkNodeIndex() {}

    private static NetworkNodeIndex get(ServerLevel level) {
        return INDEXES.computeIfAbsent(level.dimension(), key -> new NetworkNodeIndex());
    }

    public static void addRelay(ServerLevel level, NetworkRelayBlockEntity relay) {
        get(level).relays.put(relay.getBlockPos().immutable(), relay);
    }

    public static void removeRelay(ServerLevel level, BlockPos pos) {
        get(level).relays.remove(pos);
    }

    public static void addRouter(ServerLevel level, NetworkEndpointBlockEntity router) {
        get(level).routers.put(router.getBlockPos().immutable(), router);
    }

    public static void removeRouter(ServerLevel level, BlockPos pos) {
        get(level).routers.remove(pos);
    }

    /** Visit every live relay in the dimension. Distance/network filtering is left to the caller. */
    public static void forEachRelay(ServerLevel level, Consumer<NetworkRelayBlockEntity> consumer) {
        get(level).relays.values().forEach(relay -> {
            if (!relay.isRemoved()) consumer.accept(relay);
        });
    }

    /** Visit every live standalone wireless router or dongle in the dimension. */
    public static void forEachRouter(ServerLevel level, Consumer<NetworkEndpointBlockEntity> consumer) {
        get(level).routers.values().forEach(router -> {
            if (!router.isRemoved()) consumer.accept(router);
        });
    }
}
