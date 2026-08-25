package net.fretux.ailments.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HemorrhageScalingTest {
    @Test void bleedPotencyAndHemorrhageRateIncreaseWithArcane() {
        double zeroArcane = AilmentScaling.getPotencyMultiplierForArcane(0, AilmentType.BLEED);
        double highArcane = AilmentScaling.getPotencyMultiplierForArcane(100, AilmentType.BLEED);
        assertTrue(highArcane > zeroArcane);
    }
}
