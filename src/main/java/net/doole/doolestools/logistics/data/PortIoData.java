package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Real, scan-time input/output capability of a block, derived from the NeoForge capabilities it actually
 * exposes. For every face (plus the unsided handler) the scanner probes — with non-mutating simulate calls —
 * whether items/fluids/energy can be inserted and/or extracted there. This is the authoritative answer to
 * "what can flow in and out of this block, and through which face": Mekanism side-config, GregTech covers,
 * EnderIO I/O config and vanilla sided containers all express their real ports through those capabilities,
 * so the editor reads it instead of guessing by mod namespace and the transport engine uses it to probe the
 * most-likely-correct face first.
 *
 * <p>Packed into a {@code long}: six channels (item/fluid/energy × in/out) of 7 bits each — bits 0–5 are
 * {@link Direction#ordinal()} faces, bit 6 is the unsided ({@code null}) handler. A face a handler is
 * present for but whose direction could not be proven (e.g. an empty machine) is recorded as both, so the
 * block stays wireable and the runtime transport — which still falls back to a full face sweep — decides
 * correctness. The mask is never used to block transport, only to order which face is tried first.</p>
 */
public record PortIoData(long mask) {
    public static final int ITEM_IN = 0;
    public static final int ITEM_OUT = 1;
    public static final int FLUID_IN = 2;
    public static final int FLUID_OUT = 3;
    public static final int ENERGY_IN = 4;
    public static final int ENERGY_OUT = 5;

    private static final int BITS_PER_CHANNEL = 7; // 6 faces + 1 unsided
    private static final long FACE_MASK = 0x7FL;
    private static final int UNSIDED_BIT = 6;

    public static final PortIoData EMPTY = new PortIoData(0L);

    public static final Codec<PortIoData> CODEC = Codec.LONG.xmap(PortIoData::new, PortIoData::mask);

    private static int faceBit(Direction dir) { return dir == null ? UNSIDED_BIT : dir.ordinal(); }

    private long channelBits(int channel) { return (mask >>> (channel * BITS_PER_CHANNEL)) & FACE_MASK; }

    public boolean itemIn()    { return channelBits(ITEM_IN)    != 0; }
    public boolean itemOut()   { return channelBits(ITEM_OUT)   != 0; }
    public boolean fluidIn()   { return channelBits(FLUID_IN)   != 0; }
    public boolean fluidOut()  { return channelBits(FLUID_OUT)  != 0; }
    public boolean energyIn()  { return channelBits(ENERGY_IN)  != 0; }
    public boolean energyOut() { return channelBits(ENERGY_OUT) != 0; }

    /** True when at least one real port was discovered (so callers can fall back to heuristics otherwise). */
    public boolean known() { return mask != 0; }

    /** True if the given face ({@code null} = unsided) exposed this channel's direction at scan time. */
    public boolean has(int channel, Direction dir) {
        return (channelBits(channel) & (1L << faceBit(dir))) != 0;
    }

    /**
     * Faces (sided first, then the unsided {@code null}) the scan saw expose this channel's direction —
     * used by the transport engine to probe the most-likely face before its full sweep. Empty when nothing
     * was recorded, so callers keep their existing probe order unchanged.
     */
    public List<Direction> facesFor(int channel) {
        long bits = channelBits(channel);
        if (bits == 0) return List.of();
        List<Direction> faces = new ArrayList<>(7);
        for (Direction d : Direction.values()) if ((bits & (1L << d.ordinal())) != 0) faces.add(d);
        if ((bits & (1L << UNSIDED_BIT)) != 0) faces.add(null);
        return faces;
    }

    public static Builder builder() { return new Builder(); }

    /** Accumulates per-face directions discovered during a scan into a packed {@link PortIoData}. */
    public static final class Builder {
        private long mask;

        public Builder set(int channel, Direction dir) {
            mask |= (1L << faceBit(dir)) << (channel * BITS_PER_CHANNEL);
            return this;
        }

        /** Record a probed face: present-but-undetermined (neither in nor out proven) counts as both. */
        public Builder face(int inChannel, int outChannel, Direction dir, boolean in, boolean out) {
            if (!in && !out) { set(inChannel, dir); set(outChannel, dir); }
            else { if (in) set(inChannel, dir); if (out) set(outChannel, dir); }
            return this;
        }

        public PortIoData build() { return mask == 0 ? EMPTY : new PortIoData(mask); }
    }
}
