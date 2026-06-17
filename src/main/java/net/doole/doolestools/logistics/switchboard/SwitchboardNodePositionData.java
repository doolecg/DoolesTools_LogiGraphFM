package net.doole.doolestools.logistics.switchboard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SwitchboardNodePositionData(String networkId, int x, int y) {
    public static final Codec<SwitchboardNodePositionData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("networkId").forGetter(SwitchboardNodePositionData::networkId),
            Codec.INT.optionalFieldOf("x", 0).forGetter(SwitchboardNodePositionData::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(SwitchboardNodePositionData::y)
    ).apply(inst, SwitchboardNodePositionData::new));

    public SwitchboardNodePositionData sanitized() {
        return new SwitchboardNodePositionData(sanitize(networkId), clamp(x), clamp(y));
    }

    public boolean valid() {
        return networkId != null && !networkId.isBlank();
    }

    private static int clamp(int value) {
        return Math.max(-10000, Math.min(10000, value));
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        String trimmed = value.trim();
        for (int i = 0; i < trimmed.length() && out.length() < 128; i++) {
            char c = trimmed.charAt(i);
            if (!Character.isISOControl(c)) out.append(c);
        }
        return out.toString();
    }
}
