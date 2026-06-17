package net.doole.doolestools.logistics.switchboard;

public final class SwitchboardPermissions {
    private SwitchboardPermissions() {}

    public static boolean allows(boolean items, boolean fluids, boolean energy, String type) {
        return switch (type == null ? "" : type) {
            case "ITEMS" -> items;
            case "FLUIDS" -> fluids;
            case "ENERGY" -> energy;
            default -> false;
        };
    }

    public static boolean connects(String sourceNetworkId, String targetNetworkId, String a, String b) {
        if (!valid(sourceNetworkId, targetNetworkId) || a == null || b == null) return false;
        return (sourceNetworkId.equals(a) && targetNetworkId.equals(b))
                || (sourceNetworkId.equals(b) && targetNetworkId.equals(a));
    }

    public static boolean valid(String sourceNetworkId, String targetNetworkId) {
        return sourceNetworkId != null && !sourceNetworkId.isBlank()
                && targetNetworkId != null && !targetNetworkId.isBlank()
                && !sourceNetworkId.equals(targetNetworkId);
    }
}
