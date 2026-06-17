package net.doole.doolestools.logistics.lfm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogiFactoryManagerTest {
    @Test
    void legacyInPortStaysGenericSoFuelCanUseFuelSlot() {
        assertEquals(ItemTargetRole.GENERIC, ItemTargetRole.fromPort("in"));
    }

    @Test
    void namedMachinePortsStaySpecific() {
        assertEquals(ItemTargetRole.MATERIAL, ItemTargetRole.fromPort("material_in"));
        assertEquals(ItemTargetRole.FUEL, ItemTargetRole.fromPort("fuel_in"));
    }
}
