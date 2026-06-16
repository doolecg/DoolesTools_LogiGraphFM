package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Read-only snapshot of a vanilla (abstract) furnace's three slots, recipe, and progress. */
public record FurnaceSummary(boolean present,
                             String inputItem,
                             String fuelItem,
                             String outputItem,
                             int litTime,
                             int litDuration,
                             int cookProgress,
                             int cookTotal,
                             String status,
                             String inputId,
                             String resultName,
                             String resultId) {

    public static final FurnaceSummary EMPTY =
            new FurnaceSummary(false, "", "", "", 0, 0, 0, 0, "", "", "", "");

    public static final Codec<FurnaceSummary> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("present").forGetter(FurnaceSummary::present),
            Codec.STRING.fieldOf("input").forGetter(FurnaceSummary::inputItem),
            Codec.STRING.fieldOf("fuel").forGetter(FurnaceSummary::fuelItem),
            Codec.STRING.fieldOf("output").forGetter(FurnaceSummary::outputItem),
            Codec.INT.fieldOf("litTime").forGetter(FurnaceSummary::litTime),
            Codec.INT.fieldOf("litDuration").forGetter(FurnaceSummary::litDuration),
            Codec.INT.fieldOf("cookProgress").forGetter(FurnaceSummary::cookProgress),
            Codec.INT.fieldOf("cookTotal").forGetter(FurnaceSummary::cookTotal),
            Codec.STRING.fieldOf("status").forGetter(FurnaceSummary::status),
            // Optional for backward compatibility with summaries saved before recipe/icon support.
            Codec.STRING.optionalFieldOf("inputId", "").forGetter(FurnaceSummary::inputId),
            Codec.STRING.optionalFieldOf("resultName", "").forGetter(FurnaceSummary::resultName),
            Codec.STRING.optionalFieldOf("resultId", "").forGetter(FurnaceSummary::resultId)
    ).apply(inst, FurnaceSummary::new));

    public boolean hasData() {
        return present;
    }

    public boolean hasRecipe() {
        return !resultName.isEmpty();
    }

    /** True while the furnace is actively smelting (used to drive client-side live extrapolation). */
    public boolean isCooking() {
        return cookTotal > 0 && "Running".equals(status);
    }

    /**
     * Cook timer extrapolated forward by {@code elapsedTicks} since the scan. The furnace advances its
     * timer by 1 per tick and loops to the next item when full, so while cooking we wrap modulo the
     * total — the bar keeps cycling at the machine's real rate until a rescan shows it has stopped.
     */
    public int predictedProgress(long elapsedTicks) {
        if (cookTotal <= 0) return 0;
        if (!isCooking()) return cookProgress;
        long p = cookProgress + Math.max(0L, elapsedTicks);
        return (int) (p % cookTotal);
    }

    /** Ticks remaining until the current item finishes, extrapolated from the scan. */
    public int predictedRemainingTicks(long elapsedTicks) {
        return Math.max(0, cookTotal - predictedProgress(elapsedTicks));
    }

    public int cookPercent() {
        if (cookTotal <= 0) return 0;
        return (int) Math.round((cookProgress * 100.0) / cookTotal);
    }

    public int predictedPercent(long elapsedTicks) {
        if (cookTotal <= 0) return 0;
        return (int) Math.round((predictedProgress(elapsedTicks) * 100.0) / cookTotal);
    }

    public int burnPercent() {
        if (litDuration <= 0) return 0;
        return (int) Math.round((litTime * 100.0) / litDuration);
    }
}
