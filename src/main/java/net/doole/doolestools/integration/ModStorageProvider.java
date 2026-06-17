package net.doole.doolestools.integration;

import net.doole.doolestools.logistics.ScannedType;
import net.doole.doolestools.logistics.data.EnergySummary;
import net.doole.doolestools.logistics.data.FluidSummary;
import net.doole.doolestools.logistics.data.InventorySummary;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Optional scanner extension for a specific mod's storage or machine types.
 * Return null to indicate this provider can't handle the block.
 * Must be safe to call during a scan - no mutations, no chunk loading.
 */
public interface ModStorageProvider {

    /** True if this provider can read the given block entity. */
    boolean canHandle(BlockEntity be);

    /**
     * Return extra inventory/fluid/energy data for the block,
     * or null if reading failed. The scanner merges this with its normal read.
     */
    @Nullable
    ModStorageResult read(ServerLevel level, BlockEntity be);

    record ModStorageResult(
        @Nullable InventorySummary inventory,
        @Nullable FluidSummary fluids,
        @Nullable EnergySummary energy,
        ScannedType typeOverride
    ) {}
}
