package net.doole.doolestools.logistics.easyfactory;

enum ItemTargetRole {
    MATERIAL,
    FUEL,
    GENERIC;

    static ItemTargetRole fromPort(String targetPortId) {
        String port = targetPortId == null ? "" : targetPortId.toLowerCase(java.util.Locale.ROOT);
        if (port.contains("fuel")) return FUEL;
        if (port.contains("material") || port.contains("input") || port.endsWith("_in")) return MATERIAL;
        return GENERIC;
    }
}
