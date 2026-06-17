package net.doole.doolestools.integration.ae2;

import net.doole.doolestools.integration.IntegrationHooks;
import net.doole.doolestools.integration.ModStorageProvider;
import net.doole.doolestools.logistics.ScannedType;
import net.doole.doolestools.logistics.data.FluidSummary;
import net.doole.doolestools.logistics.data.EnergySummary;
import net.doole.doolestools.logistics.data.InventorySummary;
import net.doole.doolestools.logistics.data.ItemEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads AE2 drives and storage cells via NeoForge item capability.
 * AE2 already exposes its drive/terminal inventory through the capability system, so
 * we dont need deep reflection - just probe every face and collect what we find.
 * If ae2 isn't loaded this whole thing is a no-op.
 */
public final class AE2StorageProvider implements ModStorageProvider {

    // cached once at construction time - wont change at runtime
    private final boolean ae2Loaded = IntegrationHooks.isLoaded("ae2") || IntegrationHooks.isLoaded("appliedenergistics2");

    // the ae2 block entity base class, looked up lazily so we dont blow up if ae2 isnt present
    private volatile boolean classLookupAttempted = false;
    private volatile Class<?> ae2BeClass = null;

    @Override
    public boolean canHandle(BlockEntity be) {
        if (!ae2Loaded) return false;
        return isAe2BlockEntity(be);
    }

    @Override
    public @Nullable ModStorageResult read(ServerLevel level, BlockEntity be) {
        try {
            BlockPos pos = be.getBlockPos();
            BlockState state = level.getBlockState(pos);

            // AE2 drives/cells already expose items via the NeoForge capability.
            // Walk all faces (and null side) to get the richest view we can.
            ResourceHandler<ItemResource> handler = findItemHandler(level, pos, state, be);
            if (handler == null) return null;

            int slots = Math.max(0, handler.size());
            if (slots == 0) return null;

            int used = 0;
            Map<String, Integer> tally = new LinkedHashMap<>();
            Map<String, String> names = new LinkedHashMap<>();

            for (int i = 0; i < slots; i++) {
                ItemResource res = handler.getResource(i);
                long amount = handler.getAmountAsLong(i);
                if (res == null || res.isEmpty() || amount <= 0) continue;
                used++;
                String id = BuiltInRegistries.ITEM.getKey(res.getItem()).toString();
                int count = amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
                tally.merge(id, count, Integer::sum);
                names.putIfAbsent(id, res.getHoverName().getString());
            }

            if (tally.isEmpty()) return null;

            List<ItemEntry> top = tally.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(500)
                    .map(e -> new ItemEntry(names.get(e.getKey()), e.getKey(), e.getValue()))
                    .toList();

            InventorySummary inv = new InventorySummary(used, slots, top);
            // AE2 drives are storage, not machines
            return new ModStorageResult(inv, FluidSummary.EMPTY, EnergySummary.EMPTY, ScannedType.STORAGE);

        } catch (Exception e) {
            // safe fallback - a borken ae2 read shouldn't take the whole scan down
            return null;
        }
    }

    private boolean isAe2BlockEntity(BlockEntity be) {
        // lazy class lookup so we only pay the reflection cost once
        if (!classLookupAttempted) {
            synchronized (this) {
                if (!classLookupAttempted) {
                    try {
                        // AE2's internal BE base - the actual class name varies by version so
                        // we try a few candidates
                        ae2BeClass = tryLoadClass(
                                "appeng.blockentity.AEBaseBlockEntity",
                                "appeng.blockentity.AEBaseInvBlockEntity",
                                "appeng.tile.AEBaseTile"
                        );
                    } catch (Exception ignored) {
                        ae2BeClass = null;
                    }
                    classLookupAttempted = true;
                }
            }
        }

        if (ae2BeClass != null) {
            return ae2BeClass.isInstance(be);
        }

        // fallback: check package name if we couldnt find the class
        String className = be.getClass().getName();
        return className.startsWith("appeng.") || className.contains(".ae2.");
    }

    @Nullable
    private static Class<?> tryLoadClass(String... candidates) {
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
                // try the next one
            }
        }
        return null;
    }

    private static final Direction[] FACES = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    @Nullable
    private static ResourceHandler<ItemResource> findItemHandler(ServerLevel level, net.minecraft.core.BlockPos pos, BlockState state, BlockEntity be) {
        for (Direction face : FACES) {
            try {
                ResourceHandler<ItemResource> h = Capabilities.Item.BLOCK.getCapability(level, pos, state, be, face);
                if (h != null) return h;
            } catch (RuntimeException ignored) {
                // some ae2 providers are face sensitive and throw on null side
            }
        }
        return null;
    }
}
