package net.doole.doolestools.logistics.switchboard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.doole.doolestools.logistics.LinkType;

public record SwitchboardLinkData(String sourceNetworkId,
                                  String targetNetworkId,
                                  boolean items,
                                  boolean fluids,
                                  boolean energy,
                                  int priority) {
    public static final Codec<SwitchboardLinkData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("sourceNetworkId").forGetter(SwitchboardLinkData::sourceNetworkId),
            Codec.STRING.fieldOf("targetNetworkId").forGetter(SwitchboardLinkData::targetNetworkId),
            Codec.BOOL.optionalFieldOf("items", false).forGetter(SwitchboardLinkData::items),
            Codec.BOOL.optionalFieldOf("fluids", false).forGetter(SwitchboardLinkData::fluids),
            Codec.BOOL.optionalFieldOf("energy", false).forGetter(SwitchboardLinkData::energy),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(SwitchboardLinkData::priority)
    ).apply(inst, SwitchboardLinkData::new));

    public boolean valid() {
        return SwitchboardPermissions.valid(sourceNetworkId, targetNetworkId);
    }

    public boolean connects(String a, String b) {
        return SwitchboardPermissions.connects(sourceNetworkId, targetNetworkId, a, b);
    }

    public boolean touches(String networkId) {
        return valid() && (sourceNetworkId.equals(networkId) || targetNetworkId.equals(networkId));
    }

    public String other(String networkId) {
        if (sourceNetworkId.equals(networkId)) return targetNetworkId;
        if (targetNetworkId.equals(networkId)) return sourceNetworkId;
        return "";
    }

    public boolean allows(LinkType type) {
        return SwitchboardPermissions.allows(items, fluids, energy, type == null ? "" : type.name());
    }

    public SwitchboardLinkData sanitized() {
        return new SwitchboardLinkData(sanitize(sourceNetworkId), sanitize(targetNetworkId), items, fluids, energy,
                Math.max(0, Math.min(999, priority)));
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
