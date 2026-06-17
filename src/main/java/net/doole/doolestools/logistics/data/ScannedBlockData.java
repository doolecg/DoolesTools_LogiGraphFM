package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.doole.doolestools.logistics.ScannedType;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * An immutable, read-only snapshot of a single block discovered during a scan.
 * Produced server-side by the scanner and synced to the client GUI.
 */
public record ScannedBlockData(String id,
                               BlockPos pos,
                               String dimension,
                               String blockName,
                               String registryId,
                               ScannedType type,
                               double distance,
                               InventorySummary inventory,
                               FluidSummary fluids,
                               EnergySummary energy,
                               FurnaceSummary furnace,
                                MachineProgressData progress,
                                List<WarningData> warnings,
                                long lastScannedGameTime,
                                String networkId,
                                String networkName) {

    public static final Codec<ScannedBlockData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("id").forGetter(ScannedBlockData::id),
            BlockPos.CODEC.fieldOf("pos").forGetter(ScannedBlockData::pos),
            Codec.STRING.fieldOf("dimension").forGetter(ScannedBlockData::dimension),
            Codec.STRING.fieldOf("blockName").forGetter(ScannedBlockData::blockName),
            Codec.STRING.fieldOf("registryId").forGetter(ScannedBlockData::registryId),
            ScannedType.CODEC.fieldOf("type").forGetter(ScannedBlockData::type),
            Codec.DOUBLE.fieldOf("distance").forGetter(ScannedBlockData::distance),
            InventorySummary.CODEC.fieldOf("inventory").forGetter(ScannedBlockData::inventory),
            FluidSummary.CODEC.fieldOf("fluids").forGetter(ScannedBlockData::fluids),
            EnergySummary.CODEC.fieldOf("energy").forGetter(ScannedBlockData::energy),
            FurnaceSummary.CODEC.fieldOf("furnace").forGetter(ScannedBlockData::furnace),
            MachineProgressData.CODEC.optionalFieldOf("progress", MachineProgressData.EMPTY).forGetter(ScannedBlockData::progress),
            WarningData.CODEC.listOf().fieldOf("warnings").forGetter(ScannedBlockData::warnings),
            Codec.LONG.fieldOf("lastScannedGameTime").forGetter(ScannedBlockData::lastScannedGameTime),
            Codec.STRING.optionalFieldOf("networkId", "").forGetter(ScannedBlockData::networkId),
            Codec.STRING.optionalFieldOf("networkName", "").forGetter(ScannedBlockData::networkName)
    ).apply(inst, ScannedBlockData::new));

    public ScannedBlockData(String id, BlockPos pos, String dimension, String blockName, String registryId,
                            ScannedType type, double distance, InventorySummary inventory, FluidSummary fluids,
                            EnergySummary energy, FurnaceSummary furnace, List<WarningData> warnings,
                            long lastScannedGameTime) {
        this(id, pos, dimension, blockName, registryId, type, distance, inventory, fluids, energy, furnace,
                MachineProgressData.EMPTY, warnings, lastScannedGameTime, "", "");
    }

    public ScannedBlockData withProgress(MachineProgressData progress) {
        return new ScannedBlockData(id, pos, dimension, blockName, registryId, type, distance, inventory,
                fluids, energy, furnace, progress, warnings, lastScannedGameTime, networkId, networkName);
    }

    public ScannedBlockData withNetworkIdentity(String newId, String newName) {
        return new ScannedBlockData(newId, pos, dimension, newName, registryId, type, distance, inventory,
                fluids, energy, furnace, progress, warnings, lastScannedGameTime, networkId, networkName);
    }

    public ScannedBlockData withNetworkSource(String sourceNetworkId, String sourceNetworkName) {
        return new ScannedBlockData(id, pos, dimension, blockName, registryId, type, distance, inventory,
                fluids, energy, furnace, progress, warnings, lastScannedGameTime,
                sourceNetworkId == null ? "" : sourceNetworkId,
                sourceNetworkName == null || sourceNetworkName.isBlank() ? sourceNetworkId == null ? "" : sourceNetworkId : sourceNetworkName);
    }

    public boolean isStorageLike() {
        return type == ScannedType.STORAGE || type == ScannedType.UNKNOWN_STORAGE;
    }

    public boolean isMachineLike() {
        return type == ScannedType.MACHINE || type == ScannedType.UNKNOWN_MACHINE;
    }

    public boolean isUnknown() {
        return type == ScannedType.UNKNOWN
                || type == ScannedType.UNKNOWN_STORAGE
                || type == ScannedType.UNKNOWN_MACHINE;
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
