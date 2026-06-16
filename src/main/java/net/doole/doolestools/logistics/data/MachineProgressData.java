package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Read-only machine progress derived from public machine data or scan-to-scan deltas. */
public record MachineProgressData(boolean present,
                                  String label,
                                  int percent,
                                  long remainingTicks,
                                  boolean active,
                                  boolean error,
                                  String status) {
    public static final MachineProgressData EMPTY = new MachineProgressData(false, "", 0, -1L, false, false, "");

    public static final Codec<MachineProgressData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("present").forGetter(MachineProgressData::present),
            Codec.STRING.fieldOf("label").forGetter(MachineProgressData::label),
            Codec.INT.fieldOf("percent").forGetter(MachineProgressData::percent),
            Codec.LONG.fieldOf("remainingTicks").forGetter(MachineProgressData::remainingTicks),
            Codec.BOOL.fieldOf("active").forGetter(MachineProgressData::active),
            Codec.BOOL.fieldOf("error").forGetter(MachineProgressData::error),
            Codec.STRING.fieldOf("status").forGetter(MachineProgressData::status)
    ).apply(inst, MachineProgressData::new));

    public boolean hasTimer() {
        return remainingTicks >= 0;
    }
}
