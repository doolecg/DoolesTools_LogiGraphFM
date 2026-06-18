package net.doole.doolestools.logistics.switchboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchboardCleanupTest {
    @Test
    void removingNetworkDropsLinksAndNodePositionForThatNetwork() {
        assertTrue(SwitchboardCleanup.linkTouchesNetwork("net_a", "net_b", "net_b"));
        assertTrue(SwitchboardCleanup.linkTouchesNetwork("net_b", "net_c", "net_b"));
        assertFalse(SwitchboardCleanup.linkTouchesNetwork("net_c", "net_d", "net_b"));
        assertTrue(SwitchboardCleanup.nodeMatchesNetwork("net_b", "net_b"));
        assertFalse(SwitchboardCleanup.nodeMatchesNetwork("net_c", "net_b"));
    }
}
