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
    void speedItemLimitDoublesPerUpgradeAndStackGoesAbove64() {
        // base 4, no upgrades
        assertEquals(4, WirelessNetworkPolicy.speedItemLimit(4, 0, 0));
        // doubles each speed upgrade: 8 → 16 → 32 → 64, caps at one stack
        assertEquals(8,  WirelessNetworkPolicy.speedItemLimit(4, 1, 0));
        assertEquals(16, WirelessNetworkPolicy.speedItemLimit(4, 2, 0));
        assertEquals(32, WirelessNetworkPolicy.speedItemLimit(4, 3, 0));
        assertEquals(64, WirelessNetworkPolicy.speedItemLimit(4, 4, 0));
        // clamped — 5+ speed upgrades do not exceed 64
        assertEquals(64, WirelessNetworkPolicy.speedItemLimit(4, 8, 0));
        // stack upgrades add 64 per card, allowing beyond one stack
        assertEquals(128, WirelessNetworkPolicy.speedItemLimit(4, 4, 1));
        assertEquals(256, WirelessNetworkPolicy.speedItemLimit(4, 4, 3));
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
