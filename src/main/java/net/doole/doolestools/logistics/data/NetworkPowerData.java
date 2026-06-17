package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record NetworkPowerData(int supplyCentiFe,
                               int demandCentiFe,
                               int computerCentiFe,
                               int endpointCentiFe,
                               int wireCentiFe,
                               int deviceCentiFe,
                               int routeCentiFe,
                               int endpointCount,
                               int wireCount,
                               int deviceCount,
                               int routeCount,
                               int batteryCentiFe,
                               long batteryStored,
                               long batteryCapacity,
                               int batteryCount,
                               int generatorCount) {
    public static final NetworkPowerData EMPTY = new NetworkPowerData(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0L, 0L, 0, 0);

    public static final Codec<NetworkPowerData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("supplyCentiFe").forGetter(NetworkPowerData::supplyCentiFe),
            Codec.INT.fieldOf("demandCentiFe").forGetter(NetworkPowerData::demandCentiFe),
            Codec.INT.fieldOf("computerCentiFe").forGetter(NetworkPowerData::computerCentiFe),
            Codec.INT.fieldOf("endpointCentiFe").forGetter(NetworkPowerData::endpointCentiFe),
            Codec.INT.fieldOf("wireCentiFe").forGetter(NetworkPowerData::wireCentiFe),
            Codec.INT.fieldOf("deviceCentiFe").forGetter(NetworkPowerData::deviceCentiFe),
            Codec.INT.fieldOf("routeCentiFe").forGetter(NetworkPowerData::routeCentiFe),
            Codec.INT.fieldOf("endpointCount").forGetter(NetworkPowerData::endpointCount),
            Codec.INT.fieldOf("wireCount").forGetter(NetworkPowerData::wireCount),
            Codec.INT.fieldOf("deviceCount").forGetter(NetworkPowerData::deviceCount),
            Codec.INT.fieldOf("routeCount").forGetter(NetworkPowerData::routeCount),
            Codec.INT.optionalFieldOf("batteryCentiFe", 0).forGetter(NetworkPowerData::batteryCentiFe),
            Codec.LONG.optionalFieldOf("batteryStored", 0L).forGetter(NetworkPowerData::batteryStored),
            Codec.LONG.optionalFieldOf("batteryCapacity", 0L).forGetter(NetworkPowerData::batteryCapacity),
            Codec.INT.optionalFieldOf("batteryCount", 0).forGetter(NetworkPowerData::batteryCount),
            Codec.INT.optionalFieldOf("generatorCount", 0).forGetter(NetworkPowerData::generatorCount)
    ).apply(inst, NetworkPowerData::new));

    public boolean powered() {
        return supplyCentiFe >= demandCentiFe;
    }

    public int deficitCentiFe() {
        return Math.max(0, demandCentiFe - supplyCentiFe);
    }

    /** How well supply meets demand, in [0,1]. 1 = fine, a fraction = brownout, 0 = no power. */
    public float satisfaction() {
        if (demandCentiFe <= 0) return 1f;
        if (supplyCentiFe <= 0) return 0f;
        return Math.min(1f, (float) supplyCentiFe / demandCentiFe);
    }

    /** Running, but supply can't fully meet demand — routing throttles down (slow). */
    public boolean degraded() {
        return demandCentiFe > 0 && supplyCentiFe > 0 && supplyCentiFe < demandCentiFe;
    }

    /** No usable power for a non-zero demand — automation stops entirely. */
    public boolean starved() {
        return demandCentiFe > 0 && supplyCentiFe <= 0;
    }
}
