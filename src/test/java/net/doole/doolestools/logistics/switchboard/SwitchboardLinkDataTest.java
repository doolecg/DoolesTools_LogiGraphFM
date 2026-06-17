package net.doole.doolestools.logistics.switchboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchboardLinkDataTest {
    @Test
    void linkAllowsOnlyEnabledResourceTypes() {
        assertTrue(SwitchboardPermissions.allows(true, false, true, "ITEMS"));
        assertFalse(SwitchboardPermissions.allows(true, false, true, "FLUIDS"));
        assertTrue(SwitchboardPermissions.allows(true, false, true, "ENERGY"));
        assertFalse(SwitchboardPermissions.allows(true, false, true, "MANUAL"));
    }

    @Test
    void linkMatchesBothDirections() {
        assertTrue(SwitchboardPermissions.connects("net_a", "net_b", "net_a", "net_b"));
        assertTrue(SwitchboardPermissions.connects("net_a", "net_b", "net_b", "net_a"));
        assertFalse(SwitchboardPermissions.connects("net_a", "net_b", "net_a", "net_c"));
    }
}
