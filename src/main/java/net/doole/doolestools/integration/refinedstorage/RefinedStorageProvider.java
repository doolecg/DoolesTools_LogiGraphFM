package net.doole.doolestools.integration.refinedstorage;

import net.doole.doolestools.integration.IntegrationHooks;
import net.doole.doolestools.integration.ModStorageProvider;
import net.doole.doolestools.logistics.ScannedType;
import net.doole.doolestools.logistics.data.EnergySummary;
import net.doole.doolestools.logistics.data.FluidSummary;
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

public final class RefinedStorageProvider implements ModStorageProvider {
    private final boolean loaded = IntegrationHooks.isLoaded("refinedstorage");

    @Override
    public boolean canHandle(BlockEntity be) {
        if (!loaded) return false;
        String className = be.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return className.contains("refinedstorage") || className.startsWith("com.refinedmods.");
    }

    @Override
    public @Nullable ModStorageResult read(ServerLevel level, BlockEntity be) {
        ResourceHandler<ItemResource> handler = findItemHandler(level, be.getBlockPos(), level.getBlockState(be.getBlockPos()), be);
        if (handler == null || handler.size() <= 0) return null;
        Map<String, Integer> tally = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        int used = 0;
        for (int i = 0; i < handler.size(); i++) {
            ItemResource res = handler.getResource(i);
            long amount = handler.getAmountAsLong(i);
            if (res == null || res.isEmpty() || amount <= 0) continue;
            used++;
            String id = BuiltInRegistries.ITEM.getKey(res.getItem()).toString();
            tally.merge(id, amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount, Integer::sum);
            names.putIfAbsent(id, res.getHoverName().getString());
        }
        if (tally.isEmpty()) return null;
        List<ItemEntry> items = tally.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(500)
                .map(e -> new ItemEntry(names.get(e.getKey()), e.getKey(), e.getValue()))
                .toList();
        return new ModStorageResult(new InventorySummary(used, handler.size(), items), FluidSummary.EMPTY, EnergySummary.EMPTY, ScannedType.STORAGE);
    }

    private static ResourceHandler<ItemResource> findItemHandler(ServerLevel level, BlockPos pos, BlockState state, BlockEntity be) {
        for (Direction face : new Direction[] { null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST }) {
            try {
                ResourceHandler<ItemResource> handler = Capabilities.Item.BLOCK.getCapability(level, pos, state, be, face);
                if (handler != null) return handler;
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }
}
