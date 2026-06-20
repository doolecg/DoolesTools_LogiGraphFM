package net.doole.doolestools.logistics;

import net.doole.doolestools.logistics.data.GraphPortData;
import net.doole.doolestools.logistics.data.PortIoData;
import net.doole.doolestools.logistics.data.ScannedBlockData;

import java.util.List;

/** Infers planning ports for scanned blocks. Future mod integrations should plug in here. */
public final class PortDiscovery {
    private PortDiscovery() {}

    public static final GraphPortData GENERIC_IN = new GraphPortData("in", PortDirection.IN, PortKind.GENERIC, "Input");
    public static final GraphPortData GENERIC_OUT = new GraphPortData("out", PortDirection.OUT, PortKind.GENERIC, "Output");

    public static List<GraphPortData> discover(ScannedBlockData scanned) {
        if (scanned == null) return fallback();
        PortIoData io = scanned.ports();
        if (io != null && io.known()) return realPorts(scanned, io);
        if (scanned.furnace().hasData()) {
            java.util.ArrayList<GraphPortData> ports = new java.util.ArrayList<>(List.of(
                    new GraphPortData("material_in", PortDirection.IN, PortKind.ITEM, "Material"),
                    new GraphPortData("fuel_in", PortDirection.IN, PortKind.ITEM, "Fuel"),
                    new GraphPortData("item_out", PortDirection.OUT, PortKind.ITEM, "Output")
            ));
            addFluidEnergyPorts(scanned, ports);
            return ports;
        }
        java.util.ArrayList<GraphPortData> ports = new java.util.ArrayList<>();
        String namespace = namespace(scanned.registryId());
        if (scanned.isMachineLike()) {
            // Every machine reads like a furnace: a material input, a fuel/power input, and an output.
            ports.add(new GraphPortData("material_in", PortDirection.IN, PortKind.ITEM, "Input"));
            if (scanned.energy().hasData()) {
                ports.add(new GraphPortData("power_in", PortDirection.IN, PortKind.ENERGY, "Power"));
            } else {
                ports.add(new GraphPortData("fuel_in", PortDirection.IN, PortKind.ITEM, "Fuel"));
            }
            ports.add(new GraphPortData("item_out", PortDirection.OUT, PortKind.ITEM, "Output"));
            if (scanned.fluids().hasData()) {
                ports.add(new GraphPortData("fluid_in", PortDirection.IN, PortKind.FLUID, "Fluid"));
                ports.add(new GraphPortData("fluid_out", PortDirection.OUT, PortKind.FLUID, "Fluid"));
            }
            addModSpecificPorts(namespace, ports);
            return ports;
        }
        if (scanned.isStorageLike() || scanned.type() == ScannedType.TRANSPORT) {
            ports.add(new GraphPortData("item_in", PortDirection.IN, PortKind.ITEM, "Items"));
            ports.add(new GraphPortData("item_out", PortDirection.OUT, PortKind.ITEM, "Items"));
        }
        addFluidEnergyPorts(scanned, ports);
        if (!ports.isEmpty()) return ports;
        return fallbackFor(scanned);
    }

    /**
     * Build planning ports from the real per-face IO the scanner probed off the block's capabilities.
     * This is the authoritative path (Mekanism side-config, GregTech covers, EnderIO I/O, vanilla sided
     * containers all surface here); the namespace heuristics below are only the fallback for blocks that
     * expose no standard item/fluid/energy capability (e.g. AE2/RS network devices).
     */
    private static List<GraphPortData> realPorts(ScannedBlockData scanned, PortIoData io) {
        java.util.ArrayList<GraphPortData> ports = new java.util.ArrayList<>();
        boolean machine = scanned.isMachineLike();
        if (scanned.furnace().hasData()) {
            // Keep the furnace's semantic split so material vs. fuel routing still works in the planner.
            ports.add(new GraphPortData("material_in", PortDirection.IN, PortKind.ITEM, "Material"));
            ports.add(new GraphPortData("fuel_in", PortDirection.IN, PortKind.ITEM, "Fuel"));
            ports.add(new GraphPortData("item_out", PortDirection.OUT, PortKind.ITEM, "Output"));
        } else {
            if (io.itemIn())  ports.add(new GraphPortData("item_in", PortDirection.IN, PortKind.ITEM, machine ? "Input" : "Items"));
            if (io.itemOut()) ports.add(new GraphPortData("item_out", PortDirection.OUT, PortKind.ITEM, machine ? "Output" : "Items"));
        }
        if (io.fluidIn())  ports.add(new GraphPortData("fluid_in", PortDirection.IN, PortKind.FLUID, "Fluid"));
        if (io.fluidOut()) ports.add(new GraphPortData("fluid_out", PortDirection.OUT, PortKind.FLUID, "Fluid"));
        if (io.energyIn())  ports.add(new GraphPortData("energy_in", PortDirection.IN, PortKind.ENERGY, "Energy"));
        if (io.energyOut()) ports.add(new GraphPortData("energy_out", PortDirection.OUT, PortKind.ENERGY, "Energy"));
        // Additive mod-aware ports for resources that aren't item/fluid/energy capabilities
        // (Mekanism chemicals, Create kinetics, storage-network access, machine redstone control).
        addModSpecificPorts(namespace(scanned.registryId()), ports);
        return ports.isEmpty() ? fallback() : ports;
    }

    private static void addFluidEnergyPorts(ScannedBlockData scanned, java.util.List<GraphPortData> ports) {
        if (scanned.fluids().hasData()) {
            ports.add(new GraphPortData("fluid_in", PortDirection.IN, PortKind.FLUID, "Fluid"));
            ports.add(new GraphPortData("fluid_out", PortDirection.OUT, PortKind.FLUID, "Fluid"));
        }
        if (scanned.energy().hasData()) {
            ports.add(new GraphPortData("energy_in", PortDirection.IN, PortKind.ENERGY, "Energy"));
            ports.add(new GraphPortData("energy_out", PortDirection.OUT, PortKind.ENERGY, "Energy"));
        }
        String namespace = namespace(scanned.registryId());
        if (isStorageNetwork(namespace)) {
            ports.add(new GraphPortData("network_in", PortDirection.IN, PortKind.NETWORK, "Network"));
            ports.add(new GraphPortData("network_out", PortDirection.OUT, PortKind.NETWORK, "Network"));
        }
        if ("computercraft".equals(namespace) || "cc_tweaked".equals(namespace)) {
            ports.add(new GraphPortData("signal_in", PortDirection.IN, PortKind.SIGNAL, "Peripheral"));
            ports.add(new GraphPortData("signal_out", PortDirection.OUT, PortKind.SIGNAL, "Signal"));
        }
    }

    private static List<GraphPortData> fallbackFor(ScannedBlockData scanned) {
        String namespace = namespace(scanned.registryId());
        if ("mekanism".equals(namespace)) {
            return List.of(
                    new GraphPortData("item_in", PortDirection.IN, PortKind.ITEM, "Items"),
                    new GraphPortData("item_out", PortDirection.OUT, PortKind.ITEM, "Items"),
                    new GraphPortData("energy_in", PortDirection.IN, PortKind.ENERGY, "Energy"),
                    new GraphPortData("energy_out", PortDirection.OUT, PortKind.ENERGY, "Energy"),
                    new GraphPortData("chemical_in", PortDirection.IN, PortKind.GENERIC, "Chemical"),
                    new GraphPortData("chemical_out", PortDirection.OUT, PortKind.GENERIC, "Chemical")
            );
        }
        if ("create".equals(namespace)) {
            return List.of(
                    new GraphPortData("item_in", PortDirection.IN, PortKind.ITEM, "Items"),
                    new GraphPortData("item_out", PortDirection.OUT, PortKind.ITEM, "Items"),
                    new GraphPortData("fluid_in", PortDirection.IN, PortKind.FLUID, "Fluid"),
                    new GraphPortData("fluid_out", PortDirection.OUT, PortKind.FLUID, "Fluid"),
                    new GraphPortData("kinetic", PortDirection.OUT, PortKind.GENERIC, "Kinetic")
            );
        }
        if (isStorageNetwork(namespace)) {
            return List.of(
                    new GraphPortData("network_in", PortDirection.IN, PortKind.NETWORK, "Network"),
                    new GraphPortData("network_out", PortDirection.OUT, PortKind.NETWORK, "Network"),
                    new GraphPortData("item_in", PortDirection.IN, PortKind.ITEM, "Items"),
                    new GraphPortData("item_out", PortDirection.OUT, PortKind.ITEM, "Items")
            );
        }
        return fallback();
    }

    private static void addModSpecificPorts(String namespace, java.util.List<GraphPortData> ports) {
        if ("mekanism".equals(namespace)) {
            ports.add(new GraphPortData("chemical_in", PortDirection.IN, PortKind.GENERIC, "Chemical"));
            ports.add(new GraphPortData("chemical_out", PortDirection.OUT, PortKind.GENERIC, "Chemical"));
        } else if ("create".equals(namespace)) {
            ports.add(new GraphPortData("kinetic", PortDirection.OUT, PortKind.GENERIC, "Kinetic"));
        } else if ("thermal".equals(namespace) || "immersiveengineering".equals(namespace)
                || "industrialforegoing".equals(namespace)) {
            ports.add(new GraphPortData("redstone_in", PortDirection.IN, PortKind.SIGNAL, "Redstone"));
        }
        if (isStorageNetwork(namespace)) {
            ports.add(new GraphPortData("network_in", PortDirection.IN, PortKind.NETWORK, "Network"));
            ports.add(new GraphPortData("network_out", PortDirection.OUT, PortKind.NETWORK, "Network"));
        }
    }

    private static boolean isStorageNetwork(String namespace) {
        return "ae2".equals(namespace) || "appliedenergistics2".equals(namespace)
                || "refinedstorage".equals(namespace) || "integrateddynamics".equals(namespace)
                || "xnet".equals(namespace) || "toms_storage".equals(namespace)
                || "storagenetwork".equals(namespace);
    }

    private static String namespace(String registryId) {
        if (registryId == null) return "";
        int idx = registryId.indexOf(':');
        return idx <= 0 ? "" : registryId.substring(0, idx);
    }

    public static List<GraphPortData> fallback() {
        return List.of(GENERIC_IN, GENERIC_OUT);
    }

    public static boolean compatible(GraphPortData source, GraphPortData target) {
        if (source == null || target == null) return false;
        if (source.direction() != PortDirection.OUT || target.direction() != PortDirection.IN) return false;
        return source.kind() == target.kind()
                || source.kind() == PortKind.GENERIC
                || target.kind() == PortKind.GENERIC;
    }

    public static LinkType linkType(GraphPortData source) {
        if (source == null) return LinkType.MANUAL;
        return switch (source.kind()) {
            case ITEM, GENERIC, NETWORK, SIGNAL -> LinkType.ITEMS;
            case FLUID -> LinkType.FLUIDS;
            case ENERGY -> LinkType.ENERGY;
        };
    }
}
