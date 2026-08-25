package net.fretux.ailments.api;

import net.fretux.ailments.compat.AscendCompat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AilmentScalingTest {
    @Test void durationCurveMatchesBalanceTargets() {
        assertEquals(1.00, AilmentScaling.getDurationMultiplierForArcane(0), 0.001);
        assertEquals(1.08, AilmentScaling.getDurationMultiplierForArcane(20), 0.01);
        assertEquals(1.15, AilmentScaling.getDurationMultiplierForArcane(40), 0.01);
        assertEquals(1.21, AilmentScaling.getDurationMultiplierForArcane(60), 0.011);
        assertEquals(1.26, AilmentScaling.getDurationMultiplierForArcane(80), 0.011);
        assertEquals(1.30, AilmentScaling.getDurationMultiplierForArcane(100), 0.001);
        assertEquals(1.30, AilmentScaling.getDurationMultiplierForArcane(500), 0.001);
    }

    @Test void absentAscendAndNullSourceAreSafeDefaults() {
        assertDoesNotThrow(() -> AscendCompat.getArcane(null));
        assertEquals(0, AscendCompat.getArcane(null));
        assertEquals(1.0, AilmentScaling.getDurationMultiplier(null), 0.0);
        assertEquals(160, AilmentScaling.scaleDuration(160, null));
    }
}
