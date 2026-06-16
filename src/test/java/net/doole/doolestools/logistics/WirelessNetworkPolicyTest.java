package net.doole.doolestools.logistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WirelessNetworkPolicyTest {
    @Test
    void effectiveRangeUsesBaseUpgradeAndHardCap() {
        assertEquals(64, WirelessNetworkPolicy.effectiveRange(32, 16, 2, 128));
        assertEquals(128, WirelessNetworkPolicy.effectiveRange(32, 64, 4, 128));
    }

    @Test
    void inRangeUsesSquaredDistanceAgainstEffectiveRange() {
        assertTrue(WirelessNetworkPolicy.inRange(32, 16, 1, 128, 48 * 48));
        assertFalse(WirelessNetworkPolicy.inRange(32, 16, 1, 128, 49 * 49));
    }

    @Test
    void itemLimitRespectsStackUpgradesAndStackCap() {
        assertEquals(16, WirelessNetworkPolicy.itemLimit(16, 0));
        assertEquals(48, WirelessNetworkPolicy.itemLimit(16, 2));
        assertEquals(64, WirelessNetworkPolicy.itemLimit(16, 8));
    }

    @Test
    void routeBudgetBonusIsStrongerForWirelessSpeed() {
        assertEquals(8, WirelessNetworkPolicy.routeBudgetBonus(1, false));
        assertEquals(16, WirelessNetworkPolicy.routeBudgetBonus(1, true));
        assertEquals(64, WirelessNetworkPolicy.routeBudgetBonus(4, true));
    }

    @Test
    void efficiencyReducesWirelessRouteSurcharge() {
        assertEquals(150, WirelessNetworkPolicy.wirelessRouteSurcharge(200, 1));
        assertEquals(50, WirelessNetworkPolicy.wirelessRouteSurcharge(200, 3));
        assertEquals(0, WirelessNetworkPolicy.wirelessRouteSurcharge(200, 4));
    }
}
